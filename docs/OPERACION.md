# Operación en producción

Guía para quien instala y mantiene el sistema en el servidor de la fundación. Supone un servidor Linux con Docker y Docker Compose 2.24 o superior.

## 1. Antes del primer despliegue

1. **DNS**: tres registros A (o AAAA) apuntando a la IP pública del servidor, por ejemplo `portal.fundacion.pe`, `intranet.fundacion.pe` y `n8n.fundacion.pe`.
2. **Firewall**: abrir solo los puertos 80 y 443 (TCP) y 443 (UDP). Ningún otro.
Los dos repositorios van uno al lado del otro en el servidor: `oncocare_backend/` y `oncocare_frontend/`. Todo se opera desde `oncocare_backend/docker/`.

3. **Variables**: `cd oncocare_backend/docker && cp .env.prod.example .env` y completar. Generar cada secreto con `openssl rand -base64 48`.
   - `ADMIN_PASSWORD_INICIAL`: contraseña del administrador (mínimo 12 caracteres). Reemplaza a la de la semilla (`Admin123!`) en el primer arranque. **Sin ella el backend no arranca**, porque la de la semilla está publicada en el README.
   - `N8N_ENCRYPTION_KEY`: no se puede cambiar después; guárdela en el gestor de contraseñas de la fundación.
   - `INTRANET_IPS_PERMITIDAS`: la IP pública de la sede (o de la VPN) en formato CIDR, por ejemplo `200.48.10.25/32`. Fuera de esas redes, la intranet responde 403.
4. **Desplegar**:

   ```bash
   docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
   docker compose -f docker-compose.yml -f docker-compose.prod.yml ps   # todos "healthy"
   ```

   Caddy obtiene los certificados TLS solo; la primera vez puede tardar un minuto.
5. Entrar a `https://<DOMINIO_INTRANET>` con `admin@threepartners.org` y la contraseña de `ADMIN_PASSWORD_INICIAL`. Crear los usuarios reales (recepción, médicos, investigador). Después puede borrar `ADMIN_PASSWORD_INICIAL` del `.env`.
6. **n8n y Telegram**: seguir `n8n/README.md` desde `https://<DOMINIO_N8N>` (el editor solo abre desde las redes de `N8N_IPS_EDITOR`). La URL HTTPS que Telegram necesita para el webhook ya es `https://<DOMINIO_N8N>/`.

Para abreviar los comandos siguientes:

```bash
alias dc='docker compose -f docker-compose.yml -f docker-compose.prod.yml'
```

## 2. Respaldos

El servicio `respaldo` ejecuta `pg_dump` de las bases `oncologia` y `n8n` todos los días a las 03:00 (hora de Lima) en `docker/respaldos`, con retención de 7 diarios, 4 semanales y 6 mensuales. El último queda en `respaldos/last/oncologia-latest.sql.gz`.

**Copia fuera del servidor (obligatoria)**: un respaldo en el mismo disco no sirve si se pierde el servidor. Programe una copia diaria de `docker/respaldos` a otro lugar (otro servidor, almacenamiento en la nube de la fundación), por ejemplo con `rclone sync ./respaldos remoto:oncologia-respaldos` en el cron del servidor. Los respaldos contienen datos de salud: el destino debe estar cifrado y con acceso restringido.

**Respaldo manual inmediato** (antes de una actualización):

```bash
dc exec respaldo /backup.sh
```

## 3. Simulacro de restauración (mensual)

```bash
scripts/probar-restauracion.sh
```

Restaura el último respaldo en una base temporal (`oncologia_simulacro`), muestra el conteo de las tablas que sostienen los indicadores comparado con la base en uso, y borra la base temporal. **No toca la base real.** Si falla, el respaldo no sirve: revisar de inmediato.

## 4. Restauración real

Solo ante pérdida o corrupción de datos. Detiene el backend y n8n mientras dura y pide confirmación:

```bash
scripts/restaurar-respaldo.sh docker/respaldos/daily/oncologia-AAAAMMDD-HHMMSS.sql.gz
```

Después, verificar en la intranet: *Estudio > Resultados* (los indicadores coinciden con el último informe) y *Auditoría*. Toda acción posterior al respaldo restaurado se pierde; anotarlo en el registro del estudio si ocurre durante una fase.

## 5. Monitoreo

```bash
dc --profile monitoreo up -d prometheus
ssh -L 9090:localhost:9090 servidor   # y agregar "ports: [127.0.0.1:9090:9090]" al servicio
```

Alertas definidas en `docker/prometheus/alertas.yml` (visibles en *Alerts*):

| Alerta | Qué significa | Qué hacer |
|---|---|---|
| `BackendCaido` | El backend no responde | `dc logs backend --tail 200` |
| `AvisosAN8nFallidos` | Avisos descartados tras 6 intentos (≈30 min) | Revisar que n8n esté activo y los workflows importados; los avisos perdidos están en la tabla `evento_saliente` (estado FALLIDO, con el error) |
| `AvisosAN8nAcumulados` | La cola de avisos crece | n8n caído o lento; los avisos se entregarán solos cuando vuelva |
| `ConsultasEscaladasSinAtender` | Consultas esperan al personal | Avisar a recepción (afecta la NCA) |
| `CitasSinDesenlace` | Citas pasadas sin marcar | Recepción debe registrar atendida / no asistió (afecta la TNS) |
| `RecordatoriosFallidos` | Telegram no entrega | Revisar el token del bot en n8n |

Sin Prometheus, el estado rápido: `dc exec backend wget -qO- localhost:9091/actuator/health`.

## 6. Logs

En producción los logs del backend son JSON (formato ECS), una línea por evento, sin datos personales (solo ids y conteos). Docker los rota: 10 MB × 5 archivos por servicio.

```bash
dc logs backend --since 1h | grep '"log.level":"ERROR"'
```

## 7. Actualizar a una versión nueva

```bash
dc exec respaldo /backup.sh            # 1. respaldo inmediato
git pull                               # 2. código nuevo
dc up -d --build                       # 3. reconstruir; Flyway aplica las migraciones al arrancar
dc ps                                  # 4. todo "healthy"
```

Si el backend no arranca después de una migración, **no** editar la migración: restaurar el respaldo del paso 1 y reportar el error.

## 8. Escalar

- **Backend**: no guarda estado (JWT); con `RATE_LIMIT_ALMACEN=redis` (por defecto en prod) el límite de solicitudes se comparte, los jobs se coordinan con ShedLock y el outbox reparte los avisos sin duplicarlos (`SKIP LOCKED`). Se puede correr más de una réplica: `dc up -d --scale backend=2`.
- **n8n**: `N8N_EXECUTIONS_MODE=queue` y `dc --profile escalado up -d` para agregar un worker.

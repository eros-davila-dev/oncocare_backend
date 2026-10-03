# Escalabilidad, seguridad, operación y calidad

Estado tras la Fase 7. La guía operativa para el servidor (despliegue, respaldos, restauración, monitoreo) está en `docs/OPERACION.md`; aquí está el *porqué* de cada decisión, para no deshacerla por accidente.

## Escalabilidad

| Aspecto | Cómo está resuelto | Dónde |
|---|---|---|
| Backend sin estado | JWT + refresh token persistido: admite varias réplicas | `SecurityConfig` |
| Jobs programados | ShedLock: cada job corre una vez aunque haya N réplicas | `SchedulingConfig`, `infrastructure/in/scheduler` |
| Rate limit | Ventana fija de 1 min. `app.rate-limit.almacen=memoria` (dev) o `redis` (prod, compartido entre réplicas, script Lua INCR+PEXPIRE atómico). Si Redis cae, cuenta en memoria: nunca deja todo abierto ni bloquea todo | `config/ratelimit/` |
| Avisos a n8n | **Outbox transaccional**: `NotificadorExternoPort` solo inserta en `evento_saliente` dentro de la transacción del caso de uso; `EventosSalientesJob` (cada 5 s) reclama con `FOR UPDATE SKIP LOCKED`, envía sin transacción abierta y reintenta 1-2-4-8-16 min; luego FALLIDO. Entrega *al menos una vez*: los workflows deben tolerar duplicados | `application/notificacion`, `EventoSalienteJdbcAdapter` |
| Caché | Solo catálogos: preguntas frecuentes activas (Caffeine, 5 min, invalidación **después del commit**). Los indicadores NO se cachean a propósito: volumen pequeño y el investigador necesita ver el dato recién capturado o anulado | `CacheConfig` |
| Indicadores | SQL agregado sobre índices parciales; si el volumen crece mucho, vistas materializadas refrescadas por job | `IndicadoresEstudioJdbcAdapter` |
| Frontend | Dos apps estáticas (portal/intranet) en nginx con caché inmutable de assets con hash | `frontend/nginx*.conf` |

No hagas: llamar a n8n por HTTP desde un caso de uso (usa `NotificadorExternoPort`); poner `@Cacheable` en indicadores; contar el rate limit en un `Map` propio.

## Seguridad y protección de datos (Ley 29733)

- **Secretos** solo por variables de entorno. En prod son obligatorios (`${VAR:?}` en `docker-compose.prod.yml`): sin ellos el despliegue no arranca.
- **Contraseña semilla** (`Admin123!`, publicada en el README): con el perfil `prod` el backend **no arranca** si el admin la conserva, salvo que se pase `ADMIN_PASSWORD_INICIAL` (≥ 12 caracteres), que la reemplaza una sola vez, cierra sesiones y lo audita (`ProtegerPasswordSemillaUseCase`).
- **Superficie expuesta en prod**: solo Caddy (80/443). PostgreSQL, Redis, backend y n8n no publican puertos. El actuator escucha en el puerto interno 9091 y ni Caddy ni nginx enrutan `/actuator`. Swagger deshabilitado en prod. Intranet y editor de n8n restringidos por IP en Caddy (`INTRANET_IPS_PERMITIDAS`, `N8N_IPS_EDITOR`); los webhooks de n8n quedan públicos porque Telegram los necesita y cada uno valida su secreto.
- **Cabeceras**: HSTS en Caddy; CSP, `X-Frame-Options`, `nosniff`, `Referrer-Policy` y `Permissions-Policy` en nginx. CORS limitado a los dominios del portal y la intranet.
- **Proxy**: `server.forward-headers-strategy=framework` en prod; nginx conserva el `X-Forwarded-Proto` de Caddy. La IP real llega al rate limit y a la auditoría.
- **Minimización**: exportaciones con código `Pnn`; Telegram solo con nombre de pila, fecha y hora; el payload del outbox se vacía al enviarse o al fallar (puede llevar un correo o un enlace con token); los logs solo llevan ids y conteos.
- **Errores**: los 4xx de Spring MVC responden con su código (no 500); solo un error real da 500, y se registra con su traza sin exponerla.
- **Auditoría** inmutable, ya extendida a exportaciones, fases, anulaciones, desenlaces y reemplazo de la contraseña semilla.

## Observabilidad

- `/actuator/prometheus` (Micrometer). Métricas de negocio en `config/MetricasNegocio` (solo conteos, sin etiquetas con datos de pacientes): `oncologia_outbox_eventos{estado}`, `oncologia_outbox_entregas_total{resultado}`, `oncologia_consultas_escaladas_pendientes` (NCA), `oncologia_citas_sin_desenlace` (TNS), `oncologia_mediciones_sospechosas` (TPR) y `oncologia_recordatorios_fallidos` (TNS).
- Prometheus opcional (`--profile monitoreo`) con alertas en `docker/prometheus/alertas.yml`; cada alerta protege un indicador o la mensajería que lo sostiene. Para recibirlas por Telegram o correo falta Alertmanager.
- Logs JSON (ECS) en prod (`logging.structured.format.console=ecs`), con rotación en Docker (10 MB × 5).

## Despliegue

```
Internet ──HTTPS──> Caddy (TLS automático, HSTS, filtro por IP)
                     ├── DOMINIO_PORTAL    → frontend:80  (portal; /api → backend:8080)
                     ├── DOMINIO_INTRANET  → frontend:81  (intranet; /api → backend:8080)
                     └── DOMINIO_N8N       → n8n:5678     (/webhook/* público; editor por IP)
Red interna: PostgreSQL (oncologia + n8n), Redis (con contraseña), backend (:8080 API, :9091 actuator),
             respaldo (pg_dump diario), prometheus (opcional)
```

- Desde `oncocare_backend/docker`: `docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build` con `.env` basado en `.env.prod.example`.
- Perfiles Spring: por defecto (dev) y `prod` (`application-prod.yml`).
- Frontend: `PORTAL_URL` / `INTRANET_URL` se inyectan como build args en `environment.ts`.
- Respaldos: `respaldo` (pg_dump diario 03:00, 7 d / 4 sem / 6 meses en `docker/respaldos`, fuera del repo). `scripts/probar-restauracion.sh` restaura en una base temporal y compara conteos; `scripts/restaurar-respaldo.sh` restaura de verdad. El procedimiento (pg_dump `--clean --if-exists --no-owner`) se probó restaurando sobre una base vacía y sobre una existente: los conteos coinciden y los triggers de solo inserción se conservan.

## CI (GitHub Actions)

`.github/workflows/ci.yml`: backend `./mvnw verify` (tests + JaCoCo + ArchUnit) con PostgreSQL de servicio; frontend `npm ci`, tests y build de portal e intranet; validación de los JSON de `n8n/workflows/`.

## Definición de "terminado" para cualquier tarea

- [ ] Compila y pasan `./mvnw verify` y `npm test` (si no se pudo ejecutar algo, se dice explícitamente). Antes de probar el jar, `./mvnw clean package`: un build incremental dejó una vez un jar que no arrancaba.
- [ ] Si toca un indicador: test de la fórmula con casos borde y test de integración del conteo.
- [ ] Migración Flyway nueva (no se editan las aplicadas) y `ddl-auto: validate` arranca sin errores.
- [ ] Endpoints documentados en OpenAPI (anotaciones `@Operation` donde no sea obvio) y protegidos por rol.
- [ ] Eventos de auditoría para acciones que cambian datos de pacientes o mediciones, o que los exportan.
- [ ] Avisos a n8n por `NotificadorExternoPort` (outbox), nunca por HTTP directo.
- [ ] UI con tokens del tema, modo oscuro correcto, usable en móvil (portal) y con estados de carga/vacío/error.
- [ ] Sin datos personales en logs, mensajes de Telegram, prompts ni etiquetas de métricas.
- [ ] Tarea marcada en `docs/PLAN_IMPLEMENTACION.md`; si cambia la operacionalización de un indicador, anotarlo en la sección "Impacto en la tesis".

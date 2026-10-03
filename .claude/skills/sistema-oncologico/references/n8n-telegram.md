# n8n + Telegram: recordatorios y chatbot por mensajería

Objetivo: atacar el ausentismo (H2) con recordatorios oportunos y sumar un canal de consultas (H3) donde el paciente ya está: su celular.

## Principio: el backend decide, n8n entrega

| Responsabilidad | Dónde |
|---|---|
| Qué citas recordar, cuándo, a quién, por qué canal, evitar duplicados | Backend (`recordatorio`, job + reglas testeadas) |
| Enviar el mensaje por Telegram, reintentar, reportar el resultado | n8n |
| Interpretar texto libre | Backend → Gemini |
| Token del bot | Solo en credenciales de n8n |

Así, si mañana se cambia Telegram por WhatsApp o SMS, solo cambia el workflow de n8n; el indicador y su trazabilidad no se tocan.

## Restricciones de Telegram que condicionan el diseño

- **Un bot no puede escribir a un usuario que no le haya iniciado conversación.** Por eso la *vinculación* es obligatoria y debe ser lo más fácil posible (QR en recepción + botón en el portal). Los pacientes sin Telegram aparecen en la agenda como "sin canal digital" para recordatorio telefónico (y eso se registra igual en `recordatorio` con canal `LLAMADA`).
- **Un bot tiene un solo webhook.** Todo update (mensajes, `/start`, botones) entra por **un único workflow router** en n8n que luego deriva.
- El webhook necesita **HTTPS público**. En desarrollo: túnel (`cloudflared tunnel` o ngrok) y `WEBHOOK_URL` apuntando a esa URL; en producción, dominio con TLS detrás del proxy.
- `callback_data` de los botones admite máximo 64 bytes → usar `r:<recordatorioId>:<accion>`.
- Límites: ~1 mensaje/s por chat y ~30 mensajes/s global. Enviar en lotes (`Split In Batches` + `Wait`).

## Modelo de datos (implementado en `V8__telegram_recordatorios.sql`; el contrato vigente está en `n8n/README.md`)

```sql
ALTER TABLE paciente
    ADD COLUMN telegram_chat_id       BIGINT UNIQUE,
    ADD COLUMN telegram_vinculado_en  TIMESTAMPTZ,
    ADD COLUMN acepta_recordatorios   BOOLEAN NOT NULL DEFAULT TRUE;

-- token de vinculacion: reutilizar token_accion_cuenta con tipo VINCULAR_TELEGRAM
-- (hash, expira en 15 min, un solo uso). Para pacientes sin cuenta de portal,
-- recepcion genera el token desde la ficha del paciente (QR impreso/mostrado).

CREATE TABLE recordatorio (
    id                   BIGSERIAL PRIMARY KEY,
    cita_id              BIGINT NOT NULL REFERENCES cita (id),
    tipo                 VARCHAR(10) NOT NULL,     -- T72H | T24H | T2H
    canal                VARCHAR(15) NOT NULL,     -- TELEGRAM | EMAIL | LLAMADA
    programado_para      TIMESTAMPTZ NOT NULL,
    estado               VARCHAR(12) NOT NULL,     -- PENDIENTE | EN_PROCESO | ENVIADO | FALLIDO | CANCELADO
    intentos             INTEGER NOT NULL DEFAULT 0,
    mensaje_externo_id   VARCHAR(50),
    enviado_en           TIMESTAMPTZ,
    respuesta            VARCHAR(15),              -- CONFIRMO | CANCELO | PIDIO_REPROGRAMAR
    respondido_en        TIMESTAMPTZ,
    error                TEXT,
    UNIQUE (cita_id, tipo, canal)
);
CREATE INDEX idx_recordatorio_pendiente ON recordatorio (estado, programado_para);
```

La tabla `notificacion` actual queda como histórico; los nuevos envíos se registran en `recordatorio`. Al cancelar o reprogramar una cita, sus recordatorios `PENDIENTE` pasan a `CANCELADO` (y la cita nueva genera los suyos).

## Contratos backend ↔ n8n

Todos con cabecera `X-Webhook-Secret` (y opcionalmente lista de IPs permitidas).

| Llamada | Request | Response |
|---|---|---|
| `GET /api/v1/recordatorios/pendientes?limite=50` | — | `[{recordatorioId, chatId, nombrePila, fechaTexto, horaTexto, sede, tipo}]` y marca `EN_PROCESO` (con lock `FOR UPDATE SKIP LOCKED` para que dos ejecuciones no tomen el mismo) |
| `POST /api/v1/recordatorios/{id}/resultado` | `{estado: ENVIADO|FALLIDO, mensajeExternoId?, error?}` | 204 |
| `POST /api/v1/integraciones/telegram/vincular` | `{token, chatId, username?}` | `{ok, nombrePila}` o 404 si token inválido/expirado |
| `POST /api/v1/integraciones/telegram/accion-cita` | `{chatId, recordatorioId, accion: CONFIRMAR|CANCELAR|REPROGRAMAR}` | `{mensaje}` para responder al usuario. El backend verifica que `chatId` sea el del paciente dueño de la cita |
| `POST /api/v1/chatbot/mensaje` | `{mensaje, sesionId: "tg-<chatId>", canal: TELEGRAM, telegramChatId}` | `{respuesta, consultaId, sugerencias?}` |
| `POST /api/v1/chatbot/consultas/{id}/valoracion` | `{valor: 1|-1, telegramChatId}` | 204 |

`REPROGRAMAR` responde con un enlace al portal (`/mis-citas?reprogramar=<id>`) o continúa en el chat preguntando nueva fecha; ambos casos quedan como consulta en NCA.

## Workflows (carpeta `n8n/workflows/`)

1. **`telegram-router.json`** — Telegram Trigger (updates `message`, `callback_query`) → Switch:
   - Texto `/start <token>` → `vincular` → responde "¡Listo, {nombre}! Te recordaremos tus citas por aquí."
   - `/start` sin token → instrucciones para vincular.
   - `callback_query` con `r:` → `accion-cita` → `answerCallbackQuery` + editar el mensaje original con el resultado (evita doble clic).
   - `callback_query` con `v:` (👍/👎) → `valoracion`.
   - Texto libre → `chatbot/mensaje` → `sendMessage` con la respuesta y teclado 👍/👎.
   - Chat no vinculado que escribe texto → el chatbot responde en modo visitante (preguntas generales) y ofrece vincularse.
2. **`recordatorios-telegram.json`** — Schedule cada 15 min → token/secreto → `GET /recordatorios/pendientes` → lotes → `sendMessage` con teclado inline `[✅ Confirmo] [📅 Reprogramar] [❌ Cancelar]` → `POST resultado` (rama de error incluida: nunca fallar en silencio).
3. **`alertas-personal.json`** — Webhook llamado por el backend cuando una consulta se escala → mensaje al grupo de Telegram del personal con enlace a la bandeja (sin datos clínicos).
4. **`error-workflow.json`** (existe) — completar el nodo placeholder con envío al grupo técnico de Telegram.
5. **Correo** (verificación, recuperación de contraseña, bienvenida): ya **no** es de n8n. Lo envía el backend (`NotificadorCorreoPort` → outbox → `EntregaExternaEnrutador` → `ServicioCorreo`: API de correo y SMTP de respaldo, `infrastructure/out/correo`).

Convenciones: nombres de workflow `Oncologia - <Nombre>`; nodos con nombres descriptivos en español; credenciales referenciadas por nombre (`Telegram Oncologia Bot`), nunca embebidas; variables vía `$env`; exportar los JSON al repositorio tras cada cambio y probarlos importándolos en una instancia limpia.

## Contenido de los mensajes

- Solo nombre de pila, fecha, hora, sede y el tipo de atención en términos generales ("consulta", "sesión de tratamiento"). **Nunca diagnóstico, estadio, medicamentos ni DNI** (Ley 29733 y porque un celular puede verlo otra persona).
- Tono cálido, breve, en segunda persona. Ejemplo:
  > Hola María 👋 Te recordamos tu cita de **consulta** el **martes 14/10 a las 09:30** en la Fundación. ¿Podrás asistir?
- Política por defecto: 72 h (solo si la cita se agendó con más de 4 días de anticipación), 24 h y 2 h. Configurable por propiedad.

## Infraestructura de n8n (corregir en `docker-compose.yml`)

- `EXECUTIONS_MODE=queue` requiere **al menos un contenedor worker** (`command: worker`) y **PostgreSQL como base de n8n** (`DB_TYPE=postgresdb`, una base `n8n` separada). Hoy no hay worker: en modo cola los workflows quedarían encolados sin ejecutarse. Para el piloto basta `EXECUTIONS_MODE=regular`; pasar a cola cuando se escale.
- Fijar la versión de la imagen (`n8nio/n8n:<versión>`), no `latest`.
- `N8N_BASIC_AUTH_*` ya no aplica en n8n 1.x (usa gestión de usuarios propia); definir `N8N_ENCRYPTION_KEY` estable para que las credenciales sobrevivan a recrear el contenedor.
- `WEBHOOK_URL` debe ser la URL pública HTTPS (túnel o dominio).
- `GENERIC_TIMEZONE=America/Lima` y `TZ=America/Lima` para que los cron coincidan con la hora local.

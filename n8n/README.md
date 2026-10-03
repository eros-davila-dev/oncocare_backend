# n8n — Telegram: recordatorios, chatbot y alertas

n8n es el **mensajero** del sistema: traslada mensajes entre Telegram y el backend. Las reglas (qué cita recordar y cuándo, qué hacer con un botón, cómo responder una consulta) viven en el backend y están cubiertas por tests; n8n no decide nada.

| Workflow | Qué hace | Indicador |
|---|---|---|
| `telegram-router.json` | Recibe **todo** lo que llega al bot (un bot tiene un solo webhook) y lo deriva: vinculación (`/start <token>`), bienvenida, `/stop`, `/persona`, botones de recordatorio, valoraciones 👍/👎 y texto libre al chatbot | TNS, NCA |
| `recordatorios-telegram.json` | Cada 5 min pide al backend los recordatorios que ya deben salir, los envía con botones **✅ Confirmo / 📅 Reprogramar / ❌ Cancelar** e informa el resultado | TNS |
| `alertas-personal.json` | Avisa al grupo de Telegram del personal cuando el chatbot deriva una consulta (solo id y canal, sin datos del paciente) | NCA |
| `error-workflow.json` | Avisa al grupo técnico si cualquier workflow falla | — |

## Contrato con el backend

Todas las llamadas llevan el header `X-Webhook-Secret` (= `N8N_WEBHOOK_SECRET`).

| n8n → backend | Cuerpo | Respuesta |
|---|---|---|
| `GET /integraciones/recordatorios/pendientes?limite=25` | — | `[{recordatorioId, chatId, texto, fechaTexto, horaTexto, tipo}]` (el backend los marca EN_PROCESO; si no recibe resultado en 15 min, vuelven a la cola) |
| `POST /integraciones/recordatorios/{id}/resultado` | `{enviado, mensajeExternoId?, error?}` | 204 (un fallo se reintenta hasta 3 veces) |
| `POST /integraciones/telegram/vincular` | `{token, chatId}` | `{ok, mensaje}` |
| `POST /integraciones/telegram/desvincular` | `{chatId}` | `{ok, mensaje}` |
| `POST /integraciones/telegram/accion-cita` | `{chatId, recordatorioId, accion: CONFIRMAR\|CANCELAR\|REPROGRAMAR}` | `{ok, mensaje}` (verifica que la cita sea del paciente de ese chat) |
| `POST /integraciones/telegram/mensaje` | `{chatId, texto}` | `{respuesta, consultaId, estadoConsulta}` |
| `POST /integraciones/telegram/valoracion` | `{chatId, consultaId, valor: 1\|-1}` | `{ok, mensaje}` |
| `POST /integraciones/telegram/escalar` | `{chatId}` | `{ok, mensaje}` |

| backend → n8n | Cuerpo |
|---|---|
| `POST /webhook/consultas/escalada` | `{consultaId, canal}` |

Los correos (verificacion, restablecer contrasena, bienvenida) ya no pasan por n8n: los envia el backend con la API de correo o SMTP (ver el README principal, "Correo transaccional").

Los botones usan `callback_data` cortos (Telegram admite 64 bytes): `r:<recordatorioId>:<ACCION>` y `v:<consultaId>:<1|-1>`.

## Puesta en marcha

### 1. Crear el bot
1. En Telegram, habla con **@BotFather** → `/newbot` → elige nombre y usuario (p. ej. `FundacionOncoBot`). Guarda el **token** que te entrega.
2. `/setcommands` en BotFather:
   ```
   ayuda - Cómo usar el asistente
   persona - Hablar con una persona del equipo
   stop - Dejar de recibir mensajes
   ```
3. En el `.env` de la raíz: `TELEGRAM_BOT_USERNAME=FundacionOncoBot` (sin @). El backend lo usa para armar el enlace de vinculación; **el token no va en el `.env`**.

### 2. URL pública HTTPS para n8n
Telegram solo entrega mensajes a webhooks HTTPS.
- **Desarrollo**: `cloudflared tunnel --url http://localhost:5678` (o `ngrok http 5678`) y copia la URL `https://...` en `N8N_WEBHOOK_URL` del `.env`.
- **Producción**: el dominio de n8n detrás del proxy con TLS (ver `docs/PLAN_IMPLEMENTACION.md`, Fase 7).

Reinicia n8n después de cambiar `N8N_WEBHOOK_URL` (`docker compose up -d n8n`).

### 3. Grupos de Telegram para avisos
1. Crea un grupo para el personal (recepción) y otro para el equipo técnico; agrega el bot a ambos.
2. Obtén el id de cada grupo (escribe un mensaje en el grupo y abre `https://api.telegram.org/bot<TOKEN>/getUpdates`; el id del grupo es negativo, p. ej. `-1001234567890`).
3. En el `.env`: `TELEGRAM_GRUPO_PERSONAL_CHAT_ID` y `TELEGRAM_GRUPO_TECNICO_CHAT_ID`.

### 4. Credencial e importación en n8n
1. Abre n8n (`http://localhost:5678`), crea tu usuario dueño.
2. **Credentials → New → Telegram API**, nómbrala exactamente **`Telegram Oncologia Bot`** y pega el token del bot.
3. **Workflows → Import from File**, en este orden: `error-workflow.json`, `alertas-personal.json`, `recordatorios-telegram.json`, `telegram-router.json`.
4. En cada workflow abre los nodos de Telegram una vez y confirma que la credencial quedó seleccionada (al importar, n8n la asocia por nombre; si no, elígela en el desplegable).
5. En **Settings** de `recordatorios-telegram` y `telegram-router`, elige **Error Workflow → Oncologia - Error Workflow**.
6. Activa los cuatro workflows (toggle **Active**). Al activar `telegram-router`, n8n registra el webhook del bot en Telegram.

> Estos JSON se generaron siguiendo el formato de exportación de n8n 2.x, pero no se probaron contra una instancia en vivo en el entorno donde se escribieron. Tras importarlos, ejecuta las pruebas de abajo antes de usarlos con pacientes.

### 5. Probar
1. **Vinculación**: en el portal, *Mi perfil → Vincular Telegram* (o en la intranet, ficha del paciente → *QR de Telegram*). Abre el enlace en el celular y pulsa **Iniciar**. El bot debe responder «¡Listo, <nombre>!».
2. **Recordatorio**: agenda una cita para dentro de 2 h 10 min. El job del backend la programa en ≤ 15 min y n8n la envía en ≤ 5 min después del momento programado. Pulsa **✅ Confirmo**: la cita debe quedar *Confirmada* en la agenda.
3. **Chatbot**: escribe «¿cuándo es mi cita?» y luego una pregunta cuya respuesta esté en *Preguntas frecuentes*; valora con 👍/👎.
4. **Alerta**: escribe `/persona`; el grupo del personal debe recibir el aviso y la consulta debe aparecer en la *Bandeja de consultas*.

## Contenido de los mensajes (Ley 29733)
Los textos los arma el backend con **nombre de pila, fecha y hora**. Nunca diagnóstico, tratamiento, documento ni el texto de una consulta: un celular puede verlo otra persona. Las alertas al personal llevan solo el número de consulta.

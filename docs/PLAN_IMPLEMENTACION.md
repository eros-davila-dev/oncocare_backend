# Plan de implementación — Sistema web con chatbot para la gestión de pacientes

**Tesis**: *Sistema web con chatbot para la gestión de pacientes en una fundación oncológica privada de Lima, 2026* (Arnao Fretel, Dávila Anchante — UCV).
**Stack**: Java 21 · Spring Boot 3.5 · PostgreSQL 16 · Redis · Angular 21 · Tailwind CSS 4 · n8n · Telegram Bot API · Gemini.
**Guía técnica**: skill del proyecto en `.claude/skills/sistema-oncologico/` (leer `references/indicadores-tesis.md` antes de tocar cualquier métrica).

---

## 1. Objetivo del sistema, en una frase

Reducir el **tiempo promedio de registro (TPR)**, reducir la **tasa de ausentismo (TNS, línea base 65 %)** y aumentar el **nivel de consultas atendidas (NCA)**, y **medir los tres de forma automática, auditable y pareada por paciente** para contrastar H1, H2 y H3 con Wilcoxon (pretest vs. postest, n = 30).

| Indicador | Fórmula | Fuente de datos en el sistema |
|---|---|---|
| TPR (min) | Σ TRC / NCR | `medicion_registro` (sellado por el servidor; pretest por ficha manual) |
| TNS (%) | NI / (NI + NCC) × 100 | `cita` con desenlace `NO_ASISTIO` / `ATENDIDA` |
| NCA (%) | CA / TCR × 100 | `consulta` (chatbot web, Telegram; pretest: WhatsApp/llamadas por ficha manual) |

## 2. Diagnóstico del avance actual

**Lo que ya está bien construido** (se conserva):
- Backend hexagonal con JWT + refresh rotativo, bloqueo de cuenta, roles con `@PreAuthorize`, auditoría inmutable (trigger en BD), Flyway, paginación, OpenAPI, manejo de errores uniforme.
- Módulos de pacientes, citas (agendar, reprogramar, cancelar, confirmar, atendida, no asistió), tratamientos, documentos, usuarios.
- Portal de autoservicio del paciente (registro, verificación, recuperación, completar perfil, mis citas).
- Chatbot con Gemini: catálogo cerrado de intenciones, el backend ejecuta las acciones, modo degradado sin API key, historial por sesión.
- Frontend Angular standalone con signals, guards, interceptores, sistema de diseño con tokens Tailwind 4, modo oscuro, dashboard con ApexCharts.
- n8n: workflow de recordatorios por WhatsApp y sub-workflow de token.

**Brechas críticas para la tesis** (sin corregirlas, los resultados no son defendibles):

| # | Brecha | Impacto | Dónde |
|---|---|---|---|
| B1 | TPR se calcula en el navegador (`Date.now()`) y lo envía el cliente | Dato manipulable; no es "timestamp generado por el sistema" como declara el instrumento | `paciente-form.ts`, `completar-perfil.ts`, `PacienteRequestDto` |
| B2 | TPR se promedia sobre toda la tabla, sin periodo ni fase | No distingue pretest/postest | `PacienteJpaRepository.promedioTiempoRegistroSegundos` |
| B3 | TPR mide alta de paciente (1 vez por paciente) y la fórmula habla de citas registradas | Sin pares por paciente → no se puede aplicar Wilcoxon | Operacionalización |
| B4 | TNS usa como denominador **todas** las citas (incluye canceladas y futuras) | Subestima el ausentismo | `IndicadoresRepositoryAdapter` |
| B5 | "Consultas atendidas" = citas `ATENDIDA` | Mide otra cosa; H3 no tiene datos | `IndicadoresRepositoryAdapter` |
| B6 | No existe el concepto de consulta ni si fue resuelta | NCA imposible de calcular | Chatbot |
| B7 | No hay fases pretest/postest ni muestra de participantes | Indicadores no atribuibles a la muestra | — |
| B8 | No hay forma de cargar los datos del pretest en el sistema | Pretest y postest saldrían de fuentes distintas | — |
| B9 | No hay exportación pareada para SPSS | Análisis manual propenso a errores | — |

**Brechas técnicas** (las de Fase 0 ya resueltas):
- ~~El proyecto no está bajo git~~ (resuelto).
- ~~`docker-compose.yml`: n8n en modo cola sin worker~~ (resuelto en Fase 0).
- Canal de recordatorios en WhatsApp (requiere plantillas aprobadas por Meta); se reemplaza por **Telegram**.
- Una sola app Angular para pacientes y personal.
- Sin ShedLock, sin outbox hacia n8n, rate limit en memoria.

## 3. Arquitectura objetivo

```
                ┌────────────── Portal (Angular) ──────────────┐   ┌──── Intranet (Angular) ────┐
 Paciente ────▶ │ inicio · mis citas · vincular Telegram · chat │   │ dashboard · agenda · bandeja│ ◀── Personal
                └───────────────────────┬──────────────────────┘   │ estudio · pacientes · citas │
                                        │ HTTPS /api/v1             └──────────────┬─────────────┘
                                        ▼                                          ▼
                     ┌──────────────────────────── Backend Spring Boot (hexagonal) ─────────────────────────┐
                     │ pacientes · citas · agenda · chatbot+consultas · estudio/indicadores · recordatorios │
                     │ auditoría · exportación SPSS · jobs (ShedLock)                                       │
                     └───────┬───────────────────────┬────────────────────────┬──────────────────────────────┘
                             │                       │ X-Webhook-Secret       │
                        PostgreSQL / Redis        n8n (router Telegram, recordatorios, alertas) ──▶ Telegram Bot API
                                                     │                                                 ▲
                                                  Gemini (solo interpretación) ◀── backend              │
 Paciente (celular) ───────────────────────────────────────────────────────────────────────────────────┘
```

## 4. Fases

Duraciones estimadas para una persona a tiempo parcial; ajústalas a tu cronograma (Tabla 4 de la tesis). **El orden importa**: la captura del pretest (Fase 1) debe estar lista lo antes posible, porque el pretest se mide *antes* de poner el sistema en producción.

### Fase 0 — Fundaciones (≈ 1 semana) — ✅ completada
- [x] `git init`, commit del estado inicial y un commit por fase. *(Pendiente del usuario: publicar en GitHub y proteger `main`.)*
- [x] `docker-compose.yml`: n8n 2.41.5 fijado, base PostgreSQL propia para n8n (`docker/postgres/init`), modo `regular` y perfil `escalado` con worker, `N8N_ENCRYPTION_KEY`, zona Lima, `N8N_BLOCK_ENV_ACCESS_IN_NODE=false` (n8n 2.x bloquea `$env` por defecto).
- [x] Bean `Clock` (America/Lima); tablas nuevas con `TIMESTAMPTZ`.
- [x] ShedLock 6.10 + tabla `shedlock` (migración V5).
- [x] Test ArchUnit de capas hexagonales; JaCoCo. Se corrigió la única violación: casos de uso de `auth` usaban el adaptador de n8n → puerto `NotificadorExternoPort`.
- [x] GitHub Actions (`.github/workflows/ci.yml`): backend con PostgreSQL de servicio, frontend test + build, validación de JSON de n8n.
- [x] Roles `INVESTIGADOR` y `SERVICIO`.
- [x] Base de tests de integración: Testcontainers si hay Docker, o `TEST_DB_URL` apuntando a una base cuyo nombre contenga `test` (protección contra vaciar la base de desarrollo).

**Criterio de aceptación**: `docker compose up` levanta todo; CI en verde.

### Fase 1 — Módulo de estudio y corrección de indicadores (≈ 2 semanas) — *núcleo de la tesis* — ✅ completada
- [x] Migración `V6__modulo_estudio_indicadores.sql` (fases, participantes, `medicion_registro`, `consulta`, columnas nuevas de `cita`, `correccion_medicion`, triggers que impiden borrar o reescribir mediciones y consultas).
- [x] Dominio `estudio`: fórmulas puras TPR/TNS/NCA (`CalculoIndicadores`) con tests de casos borde.
- [x] Sesión de medición: `POST /mediciones/registro` (el canal lo decide el servidor según el rol); alta de paciente, actualización, completar perfil y agendar cita la cierran en su transacción; job de abandonadas cada 15 min.
- [x] Eliminado `tiempoRegistroSegundos` del cliente y de los DTO (B1, B2).
- [x] TNS corregido (B4) y NCA real sobre `consulta` (B5, B6).
- [x] Reprogramación: **decisión** — se mantiene la misma cita (cuenta una sola vez en el TNS con su desenlace final) con `veces_reprogramada`; se prohíbe reprogramar una cita ya atendida, no asistida o cancelada (antes se podía y borraba el desenlace).
- [x] Desenlace de cita con `fecha_hora_desenlace` y `desenlace_registrado_por`.
- [x] API de fases (sin solapamiento; cerrar congela) y participantes (códigos P01…, exclusión con motivo).
- [x] **Fichas del Anexo 2 digitales** + importación .xlsx/.csv (todo o nada, vista previa, plantillas, encabezados literales de la tesis aceptados).
- [x] `GET /estudio/indicadores` (fase o rango, alcance GLOBAL/MUESTRA), `/comparativo` y `/pareado`; dashboard con el mismo cálculo.
- [x] Intranet: módulo «Estudio de tesis» (resultados, fases y muestra, fichas, revisión y anulación de datos).
- [x] **Decisión**: la fase de un evento se deriva de sus fechas y de las fechas de la fase (no se guarda en cada fila); cerrar la fase congela sus resultados.

**Verificación**: 83 tests de backend (incluye integración contra PostgreSQL con un escenario conocido por indicador), 7 de frontend, build de producción y prueba de humo HTTP completa.

**Criterio de aceptación**: con un escenario sembrado conocido, los tres indicadores dan el valor esperado en tests de integración; el investigador puede empezar a capturar el pretest.

### Fase 2 — Agenda de recepción y desenlace de citas (≈ 1 semana) — *H2* — ✅ completada
- [x] `GET /citas/agenda?fecha=` (un médico solo ve sus citas) y pantalla «Agenda del día» con «Llegó» / «No asistió» (registra `fecha_hora_desenlace`, `desenlace_registrado_por`); la recepción aterriza en ella al iniciar sesión.
- [x] No se puede registrar el desenlace de una cita futura.
- [x] Bandeja de «citas pendientes de cierre» + job horario de cierre automático a las 48 h (`app.agenda.horas-cierre-automatico`, `cierre_automatico = true`).
- [x] `PATCH /citas/{id}/corregir-desenlace` con motivo obligatorio (queda en `correccion_medicion` y en la auditoría).
- [x] Medición de TPR en el formulario de nueva cita de la intranet (hecho en Fase 1).

**Verificación**: 90 tests de backend (incluye integración de las consultas de agenda y cierre), build del frontend y prueba de humo HTTP.

**Criterio de aceptación**: ninguna cita pasada queda sin desenlace más de 48 h; el TNS del dashboard coincide con un cálculo manual.

### Fase 3 — Chatbot orientado a consultas (≈ 1,5 semanas) — *H3* — ✅ completada
- [x] `ejecutarAccion` devuelve un resultado tipado (`EXITO`, `INFORMATIVA`, `REQUIERE_DATOS`, `REQUIERE_SESION`, `ERROR_NEGOCIO`, `ESCALAR`) decidido por el backend, nunca por el modelo.
- [x] `GestorConsultasChatbot`: una consulta por necesidad; continúa con la misma intención o una pregunta intercalada; cambiar de necesidad cierra la anterior como NO_RESUELTA; repetir la misma pregunta en 10 min la reabre; 4 turnos sin resolver → escalada. `conversacion_chatbot.consulta_id` enlaza cada mensaje.
- [x] Valoración 👍/👎 (👎 sobre una respuesta del bot → escalada) y «Hablar con una persona» en el widget; propiedad verificada por id de sesión.
- [x] Bandeja de consultas en la intranet (conversación + resolver / no resuelta) y job cada 10 min: abiertas sin actividad 30 min → NO_RESUELTA; escaladas sin atención 48 h → NO_RESUELTA.
- [x] Base de conocimiento (`pregunta_frecuente`, migración V7) administrable desde la intranet e inyectada en el prompt; si la respuesta no está en la información oficial, el modelo debe escalar (un «no sé» no cuenta como resuelta).
- [x] El aviso de escalamiento a n8n solo lleva id y canal (sin texto del paciente).

**Verificación**: 103 tests de backend (reglas del gestor con repositorio en memoria), build del frontend y prueba de humo HTTP con un stub local de la API de Gemini (sin consumir la clave real).

### Fase 4 — Telegram + n8n (≈ 2 semanas) — *H2 y H3* — ✅ completada (código); pendiente la puesta en marcha con el bot real
- [x] Migración `V8__telegram_recordatorios.sql` (`paciente.telegram_chat_id`, tokens de vinculación propios —muchos pacientes no tienen cuenta— y `recordatorio`).
- [x] Vinculación con token de un solo uso (30 min): enlace `t.me/<bot>?start=<token>` + QR en el portal (*Mi perfil*) y en la ficha del paciente (recepción). Un chat ↔ un paciente.
- [x] `ProgramarRecordatoriosUseCase` (job, cron configurable `RECORDATORIOS_CRON`): Telegram 72 h / 24 h / 2 h antes; sin Telegram, una llamada de recepción 24 h antes. Idempotente (UNIQUE cita+tipo+canal).
- [x] Entrega a n8n con `FOR UPDATE SKIP LOCKED`, texto armado en el backend (solo nombre de pila, fecha y hora), reintentos (3) y liberación de avisos colgados; un aviso de una cita cancelada o reprogramada se cancela en vez de enviarse.
- [x] Botones ✅ Confirmo / ❌ Cancelar (se ejecutan con verificación de que la cita es del paciente del chat) y 📅 Reprogramar (deriva una consulta al personal).
- [x] Chatbot por Telegram: consultar, confirmar y cancelar; agendar/reprogramar se derivan a recepción; chat no vinculado → preguntas generales + invitación a vincularse. Valoración 👍/👎, `/persona` y `/stop`.
- [x] Agenda: lista de llamadas de recordatorio y etiqueta «sin Telegram».
- [x] Workflows `telegram-router`, `recordatorios-telegram`, `alertas-personal` y `error-workflow` (con Telegram) + `n8n/README.md` con contrato y puesta en marcha. Se retiró el workflow de WhatsApp.
- [ ] **Pendiente del usuario**: crear el bot en @BotFather, túnel/dominio HTTPS, importar y activar los workflows en n8n y probarlos con un teléfono real (los JSON no se probaron contra una instancia viva de n8n).

**Verificación**: 115 tests de backend (incluye el ciclo completo de recordatorios contra PostgreSQL con reloj controlado), build y tests del frontend, y prueba de humo HTTP que simula a n8n (vinculación, recordatorio, botón, llamadas, chatbot, `/persona`, `/stop`). Corregido además un bug previo: el listado de citas y de tratamientos fallaba en PostgreSQL con algunos filtros nulos.

### Fase 5 — Frontend profesional: portal e intranet (≈ 2 semanas) — ✅ completada
- [x] Dos aplicaciones Angular (`portal` en 4200, `intranet` en 4300) en el mismo workspace. **Decisión**: comparten `src/app` (core, shared, features) y cada una tiene su punto de entrada (`main.portal.ts` / `main.intranet.ts`), sus rutas (`portal/portal.routes.ts`, `intranet/intranet.routes.ts`) y el token `AUDIENCIA`. Se descartó partir en `projects/*`: duplicaba configuración sin beneficio para un equipo de dos, y el bundle de cada app ya contiene solo sus pantallas (verificado).
- [x] Portal: inicio público, preguntas frecuentes, login, mis citas, mi perfil con vinculación de Telegram (QR) y widget del chatbot con valoración; mobile-first.
- [x] Intranet: dashboard de indicadores (GLOBAL + comparativo pretest/postest con el n de cada indicador y «Sin datos» en vez de 0), agenda del día, bandeja de consultas y módulo de estudio. El widget del chatbot ya no aparece en la intranet.
- [x] Cada cuenta inicia sesión solo en su aplicación (`AuthService.perteneceAEstaAplicacion`); al equivocarse de app se le muestra el enlace correcto.
- [x] Despliegue: un contenedor nginx sirve ambas apps (puertos 80 y 81) con cabeceras de seguridad (CSP, `X-Frame-Options`, `Referrer-Policy`) y proxy `/api`.
- [ ] Cliente API generado desde OpenAPI (opcional; se pospone).

**Verificación**: 115 tests de backend, 10 de frontend, build de producción de ambas apps y recorrido con Chrome headless (15 pantallas de portal e intranet, escritorio y móvil, con datos de pretest/postest cargados): 0 errores de consola. El recorrido encontró y se corrigieron dos fallos: el dashboard se rompía cuando el backend omitía `cumplimientoTratamientoPorcentaje` (ahora el DTO serializa los nulos) y «1 citas» → «1 cita». No se tomaron capturas en tema oscuro.

### Fase 6 — Exportación y análisis (≈ 1 semana) — ✅ completada
- [x] `GET /estudio/exportaciones/spss.xlsx`: hojas `pareado` (una fila por participante, celdas vacías = sin datos), `detalle` (participante × fase con los conteos), `resumen` (comparativo + Wilcoxon), `diccionario` y `metadatos`. Anónimo, sin caché y auditado.
- [x] `GET /estudio/exportaciones/fichas.xlsx?fase=`: las tres fichas del Anexo 2 fila por fila, con una hoja que recalcula cada indicador con fórmulas de Excel junto al valor del sistema. Las filas comparten con los conteos las mismas condiciones SQL.
- [x] `GET /estudio/indicadores/wilcoxon` y tarjeta en *Estudio > Resultados*. **Decisión**: la prueba se implementó en el dominio (Java puro) en vez de usar `commons-math3`, para reproducir exactamente las convenciones de SPSS (descarte de empates, corrección por empates, Z con el menor total, p exacta condicional); se validó contra scipy. La pantalla advierte que con n < 10 la p asintótica es poco fiable.
- [x] Botones de descarga en *Estudio > Resultados* (SPSS, fichas del pretest y del postest).
- [x] De paso: los errores 4xx de Spring MVC (parámetro faltante, enum inválido, JSON roto, método no permitido) respondían 500 sin registrar la traza; ahora devuelven su 4xx. La auditoría se lista de la más reciente a la más antigua.
- [ ] Shapiro-Wilk queda en SPSS (no se implementa en el sistema).

**Verificación**: 130 tests de backend (Wilcoxon contra scipy, cuadratura filas = conteos contra PostgreSQL, lectura de los .xlsx generados con evaluación de las fórmulas, manejo de errores) y 14 de frontend; prueba de humo HTTP con 6 participantes (pretest con fichas, postest con el flujo real) que abre los .xlsx con openpyxl y compara con la API, verifica que no aparezcan nombres ni documentos, 403 para MEDICO y la auditoría; descarga real desde Chrome headless y capturas en claro y oscuro.

**Criterio de aceptación**: el libro exportado se abre en SPSS sin transformaciones manuales.

### Fase 7 — Producción y robustez (≈ 1 semana) — ✅ completada (código); pendiente el despliegue en el servidor real
- [x] `docker-compose.prod.yml` (override) con Caddy: TLS automático, HSTS, tres dominios (portal, intranet restringida por IP, n8n con webhooks públicos y editor restringido). Ningún otro puerto publicado; secretos obligatorios; límites de memoria y rotación de logs. El dominio HTTPS de n8n resuelve el webhook que Telegram exige (pendiente de la Fase 4).
- [x] **Outbox** hacia n8n (`V9__outbox_eventos_salientes.sql`): los avisos se guardan en la transacción del caso de uso y un job los entrega con reintentos 1-2-4-8-16 min. Antes se enviaban por HTTP dentro de la transacción y se perdían si n8n estaba caído (afectaba el aviso de consultas escaladas, NCA).
- [x] Rate limit en Redis (script atómico, compartido entre réplicas) con respaldo en memoria si Redis cae; limpieza de ventanas vencidas en memoria; `Retry-After`.
- [x] Caché de catálogos (preguntas frecuentes; invalidación después del commit). **Decisión**: los indicadores no se cachean (volumen chico; el investigador necesita el dato fresco).
- [x] Prometheus (`/actuator/prometheus` en puerto interno) con métricas de negocio ligadas a los indicadores y alertas; logs JSON (ECS) en el perfil `prod`; Swagger deshabilitado en prod.
- [x] Respaldo diario de las bases `oncologia` y `n8n` con retención, scripts de restauración y de simulacro, y guía `docs/OPERACION.md`.
- [x] Contraseña semilla: en `prod` el backend no arranca si el admin la conserva; `ADMIN_PASSWORD_INICIAL` la reemplaza una vez (auditado).
- [x] Correo transaccional en el backend (verificación, restablecer contraseña y bienvenida al crear usuario) por la API de correo, con SMTP como respaldo opcional y entrega por el outbox. Probado contra el proveedor real. `REGISTRO_PACIENTES_HABILITADO=true` en los `.env` locales.
- [ ] **Pendiente del usuario**: servidor, DNS, `.env` de producción, primer despliegue y copia de los respaldos fuera del servidor (ver `docs/OPERACION.md`).

**Verificación**: 147 tests de backend (outbox contra PostgreSQL: rollback, reclamo sin duplicados, reintentos con reloj controlado, instancia caída y purga; caché con rollback; rate limit con Redis simulado; contraseña semilla) y 14 de frontend. Backend real con el perfil `prod`: se niega a arrancar con la contraseña semilla; con `ADMIN_PASSWORD_INICIAL`, la reemplaza; actuator solo en el puerto interno; Swagger 404; un aviso escalado con n8n caído se reintenta y llega cuando n8n vuelve; el 11.º login en un minuto da 429; el 100 % de los logs es JSON y sin contraseñas. `docker compose config` valida el compose de producción (y falla con mensaje claro si falta un secreto). Restauración probada con `pg_dump --clean --if-exists --no-owner` sobre una base vacía y sobre una existente: los conteos coinciden y los triggers de solo inserción se conservan. **No probado**: el despliegue con Docker (el daemon no estaba disponible), Caddy/TLS contra dominios reales, ni Redis real (cubierto con dobles).

### Fase 8 — Piloto (postest) y cierre (según cronograma de la tesis)
- [ ] Capacitación al personal y a los pacientes (vinculación de Telegram) — se apoya en TAM (utilidad y facilidad de uso percibidas).
- [ ] Cerrar fase PRETEST, abrir POSTEST con fechas fijas.
- [ ] Monitoreo semanal de los indicadores y de la calidad del dato (mediciones sospechosas, citas sin desenlace, consultas sin cerrar).
- [ ] Exportar, analizar en SPSS (Shapiro-Wilk, Wilcoxon) y redactar Resultados/Discusión.

## 5. Impacto en el documento de tesis (revisar con el asesor)

1. **TPR**: la fórmula dice "tiempo de registro de citas / número de citas registradas" y la dimensión "registro de pacientes". Se recomienda definir TPR sobre el registro de la atención (cita) para tener pares por paciente; el alta de paciente se reporta como descriptivo.
2. **NCA**: precisar qué es una consulta y qué es "atendida/resuelta" (ver `indicadores-tesis.md` §5); reportar también NCA_auto (solo chatbot).
3. **TNS**: precisar que canceladas con aviso y reprogramadas no cuentan.
4. **Definición operacional de la VI**: mencionar Telegram como canal de recordatorios y del chatbot (junto con Gemini y n8n, que ya figuran).
5. **Instrumento 03 postest**: el medio puede ser "Chatbot web" o "Telegram".
6. **Tecnologías**: Angular 21 (si el documento menciona 20).

## 6. Riesgos y mitigaciones

| Riesgo | Mitigación |
|---|---|
| Pacientes sin Telegram o sin smartphone | Vinculación asistida en recepción; recordatorio telefónico registrado como canal `LLAMADA`; reportar cobertura |
| Pocas consultas en el postest (n pequeño) | Promover el bot en recepción; base de preguntas frecuentes útil; registrar también consultas presenciales |
| Personal no registra desenlaces | Agenda del día simple + cierre automático a 48 h + alerta diaria |
| Caída de Gemini o cuota agotada | Modo degradado existente + escalamiento a personal (cuenta como consulta, no se pierde) |
| n8n caído | Outbox + reintentos; los recordatorios quedan `PENDIENTE` y se envían al volver |
| Retraso del desarrollo frente al cronograma | Fases 1–4 son imprescindibles para la tesis; 5–7 pueden reducirse (p. ej., una sola app con dos layouts) |

## 7. Decisiones pendientes

- [ ] Confirmar con el asesor la operacionalización de TPR (cita vs. alta de paciente) y de NCA (total vs. solo chatbot).
- [ ] Fechas de las fases pretest y postest.
- [ ] ¿La fundación tratará las cancelaciones tardías como inasistencia? (por defecto, no).
- [ ] Dominio y hosting para producción (necesario para el webhook HTTPS de Telegram).

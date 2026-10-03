---
name: sistema-oncologico
description: Guía de desarrollo del "Sistema web con chatbot para la gestión de pacientes en una fundación oncológica privada de Lima, 2026" (tesis Arnao–Dávila, UCV). Úsala SIEMPRE que se trabaje en este repositorio (backend Java 21 + Spring Boot hexagonal, frontend Angular + Tailwind 4, n8n, bot de Telegram, Gemini) y en especial cuando la tarea toque pacientes, citas, recordatorios, chatbot, consultas, dashboard, indicadores, pretest/postest, exportación a SPSS, Telegram o n8n — aunque el usuario no mencione la tesis ni los indicadores. También cuando pida "seguir con el plan", "la siguiente fase", "agregar un módulo" o "revisar si el sistema mide bien" algo.
---

# Sistema web oncológico — guía del proyecto

Este sistema existe para **demostrar con datos** tres hipótesis de una tesis preexperimental (pretest–postest, n = 30 pacientes). Cada línea de código debe ayudar a que los indicadores se midan de forma **automática, auditable y comparable** entre el antes y el después. Una funcionalidad bonita que no alimente o no proteja un indicador es secundaria; un indicador mal calculado invalida la tesis.

## 1. Los tres indicadores (la razón de ser del sistema)

| # | Dimensión | Indicador | Fórmula (Anexo 1 de la tesis) | Meta de la hipótesis |
|---|---|---|---|---|
| H1 | Registro de pacientes | **TPR** — Tiempo promedio de registro (min) | `TPR = Σ TRC / NCR` | Disminuir |
| H2 | Agendamiento de citas | **TNS** — Tasa de ausentismo / no-show (%) | `TNS = NI / (NI + NCC) × 100` | Disminuir (línea base: 65 %) |
| H3 | Eficacia de la atención | **NCA** — Nivel de consultas atendidas (%) | `NCA = CA / TCR × 100` | Aumentar |

- `TRC` tiempo de cada registro, `NCR` nº de registros; `NI` inasistencias, `NCC` citas cumplidas; `CA` consultas resueltas, `TCR` total de consultas registradas.
- Análisis estadístico de la tesis: descriptivos + Shapiro-Wilk + **Wilcoxon para muestras relacionadas**, α = 0,05, en SPSS 29. Eso implica que el sistema debe poder entregar **datos pareados por paciente** (valor pretest y postest del mismo paciente), no solo promedios globales.

Antes de tocar cualquier cosa relacionada con estas métricas, lee `references/indicadores-tesis.md`: contiene las definiciones operativas exactas (qué cuenta y qué no), el modelo de datos de medición, los casos borde y el formato de exportación. Está escrito para resolver las ambigüedades que la fórmula sola no resuelve.

### Reglas no negociables de medición

1. **Los tiempos los mide el servidor, nunca el navegador.** Un valor enviado por el cliente (`Date.now()` en Angular) es manipulable y no es "timestamp generado automáticamente por el sistema", que es lo que la tesis declara como instrumento. Se usa el patrón *sesión de medición*: el backend sella `inicio` al abrir el formulario y `fin` al persistir, en la misma transacción del guardado.
2. **El denominador importa.** TNS solo cuenta citas con desenlace (`ATENDIDA` o `NO_ASISTIO`); las canceladas con aviso, reprogramadas o aún futuras **no** entran. NCA cuenta *consultas* (una necesidad del usuario), no mensajes individuales.
3. **Toda medición lleva fase (`PRETEST`/`POSTEST`) y se puede filtrar por participante del estudio.** Los indicadores se calculan sobre la muestra (pacientes con consentimiento) y el periodo de la fase, no sobre toda la base.
4. **Nada se borra.** Las mediciones y consultas son de solo inserción (como `auditoria_accion`); las correcciones se registran como un nuevo evento con motivo y usuario.
5. **Los datos del pretest también viven en el sistema.** Se capturan con formularios del rol `INVESTIGADOR` (equivalentes digitales de las fichas del Anexo 2) para que pretest y postest salgan del mismo origen y el mismo cálculo.

## 2. Arquitectura y stack

```
Dos repositorios hermanos en C:\Users\USUARIO (cada uno con su git):
oncocare_frontend/  Angular (standalone, signals) + Tailwind CSS 4 — dos aplicaciones:
            portal (público + portal del paciente + widget chatbot) e intranet (personal)
oncocare_backend/   Java 21, Spring Boot 3.5, hexagonal: domain / application / infrastructure;
                    contiene ademas docker/ (compose dev y prod), n8n/, docs/, scripts/ y este skill
            PostgreSQL 16 + Flyway, Redis, JWT con refresh token rotativo
n8n/        Orquestación de mensajería: recordatorios y chatbot por Telegram
Telegram    Bot API: recordatorios con botones (confirmar / reprogramar / cancelar) y chat
Gemini      Solo clasifica intención y extrae entidades (JSON con responseSchema)
```

Responsabilidades, para no mezclarlas:

- **El backend decide; n8n entrega.** La lógica de *qué* recordatorio enviar, *a quién* y *cuándo* vive en el backend (tabla de recordatorios programados). n8n solo consulta los pendientes, los envía por Telegram y reporta el resultado. Así la regla es testeable con JUnit y el envío es idempotente.
- **Gemini interpreta; el backend ejecuta.** Gemini nunca toca la base de datos. La intención cae en el catálogo cerrado `Intencion`; lo demás es `ESCALATE_TO_STAFF`.
- **El token del bot de Telegram vive solo en las credenciales de n8n**, no en el backend.

Guías detalladas por capa (léelas cuando trabajes en esa capa):

| Archivo | Cuándo leerlo |
|---|---|
| `references/indicadores-tesis.md` | Cualquier cambio en citas, registro de pacientes, chatbot, consultas, dashboard, exportación, pretest/postest |
| `references/backend-hexagonal.md` | Crear o modificar un módulo, caso de uso, endpoint, migración o test del backend |
| `references/frontend-angular.md` | Pantallas, servicios, rutas, separación portal/intranet, diseño con Tailwind |
| `references/n8n-telegram.md` | Recordatorios, bot de Telegram, vinculación de cuenta, webhooks, workflows |
| `references/escalabilidad-calidad.md` | Docker, despliegue, seguridad (Ley 29733), observabilidad, CI, checklist de entrega |

El plan por fases está en `docs/PLAN_IMPLEMENTACION.md`. Si el usuario pide "seguir con el plan" o "la siguiente fase", léelo, identifica la primera tarea no marcada como hecha y continúa desde ahí.

## 3. Convenciones que ya sigue el código (respétalas)

- **Idioma**: dominio, clases, métodos, columnas y mensajes en **español sin tildes** en identificadores (`RegistrarAsistenciaCitaUseCase`, `fecha_registro`). Comentarios en español explicando el *porqué*, citando la sección del requerimiento cuando aplica.
- **Backend**: un caso de uso = una clase `XxxUseCase` en `application/<modulo>/` con `@Service`, `@PreAuthorize` por rol y `@Transactional` en el método. Puertos en `domain/<modulo>/*Port`, adaptadores en `infrastructure/out/...`, controladores en `infrastructure/in/rest/` con DTO `record` + Bean Validation + mapper MapStruct. Errores de negocio con las excepciones de `domain/shared/exception`. Auditoría vía eventos de dominio publicados con `ApplicationEventPublisher`.
- **Base de datos**: solo migraciones Flyway nuevas (la siguiente libre es `V10__...sql`); nunca editar una migración ya aplicada. `ddl-auto: validate`.
- **Frontend**: componentes standalone con `inject()`, `signal`/`computed`, formularios reactivos, servicios por feature, UI reutilizable de `shared/ui` y `shared/components`. Colores siempre con tokens semánticos del tema (`bg-primary`, `text-muted-foreground`), nunca hex sueltos.
- **Roles**: `ADMIN`, `MEDICO`, `RECEPCIONISTA`, `PACIENTE` y (nuevo) `INVESTIGADOR` para el módulo de estudio.

## 4. Flujo de trabajo para cualquier tarea

1. **Ubica la tarea en el plan** (`docs/PLAN_IMPLEMENTACION.md`) y en el indicador que afecta. Si no afecta ninguno, pregúntate si es prioritaria.
2. **Lee el código vecino** antes de escribir: copia la forma del caso de uso / componente más parecido.
3. **Diseña de dentro hacia afuera**: dominio → puerto → caso de uso → adaptador → controlador → migración → frontend.
4. **Prueba lo que mide**: todo cálculo de indicador lleva test unitario con casos borde (división por cero, citas canceladas, sesiones abandonadas, consultas escaladas). Integración con Testcontainers para las consultas SQL de indicadores.
5. **Verifica**: `./mvnw test` en backend, `npm test` y `npm run build` en frontend. Si no puedes ejecutar algo, dilo explícitamente.
6. **Actualiza el plan**: marca la tarea como hecha en `docs/PLAN_IMPLEMENTACION.md` y documenta cualquier decisión que cambie la operacionalización de un indicador (eso debe reflejarse luego en la tesis).

## 5. Señales de alerta (detente y corrige)

- Un indicador calculado con `count(*)` sobre toda la tabla sin filtrar por fase, periodo o participante.
- Un tiempo de registro que llega en el body de una petición.
- Un `UPDATE`/`DELETE` sobre `medicion_registro`, `consulta` o `auditoria_accion`.
- Lógica de negocio (qué cita recordar, cuándo marcar inasistencia) escrita dentro de un nodo de n8n.
- El chatbot ejecutando una acción fuera del catálogo `Intencion` o sin validar que la cita pertenece al paciente.
- Datos personales o clínicos en logs, en mensajes de Telegram (solo nombre de pila, fecha y hora) o en el prompt a Gemini más allá de lo imprescindible.
- Secretos en el repositorio (`.env` real, tokens de bot, API keys).

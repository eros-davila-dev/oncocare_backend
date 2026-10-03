# Indicadores de la tesis: definición operativa e implementación

Contenido:
1. Contexto metodológico que condiciona el diseño
2. Módulo de estudio (fases y participantes)
3. H1 — TPR, tiempo promedio de registro
4. H2 — TNS, tasa de ausentismo
5. H3 — NCA, nivel de consultas atendidas
6. Modelo de datos (migración propuesta)
7. Cálculo, endpoints y exportación para SPSS
8. Casos borde y tests obligatorios

---

## 1. Contexto metodológico que condiciona el diseño

- **Diseño**: preexperimental, un solo grupo, pretest–postest. Población 32, muestra 30 pacientes (no probabilístico por conveniencia).
- **Prueba**: Shapiro-Wilk y luego **Wilcoxon para muestras relacionadas** (α = 0,05). Wilcoxon compara *pares* (mismo paciente, antes y después). Por eso cada indicador necesita un **valor por paciente y por fase**, además del valor global.
- **Instrumentos (Anexo 2)**: ficha de registro de tiempos (hora inicio / hora fin), ficha de registro de ausentismo (asistió sí/no), ficha de registro del sistema (medio, consulta, resuelta sí/no). El sistema debe producir exactamente estas fichas, en digital, para ambas fases.
- **Situación pretest**: todo se llevaba en hojas de cálculo; las consultas llegaban por WhatsApp y llamadas; ausentismo del 65 %.
- **Ética (Ley 29733)**: solo participan pacientes con consentimiento informado; los datos exportados se anonimizan con un código de participante (`P01`…`P30`).

## 2. Módulo de estudio (fases y participantes)

Sin esto, ningún indicador es atribuible a la muestra ni al periodo correcto.

- `estudio_fase`: `PRETEST` y `POSTEST`, cada una con `fecha_inicio` y `fecha_fin` (y opcionalmente `estado` ABIERTA/CERRADA). Configurable por `ADMIN`/`INVESTIGADOR`.
- `participante_estudio`: vincula `paciente_id` con `codigo` (`P01`), `fecha_consentimiento`, `incluido` (bool) y `motivo_exclusion` (criterios de la tesis: suspende tratamiento, fallece, retira consentimiento, registros incompletos).
- **Implementado así**: la fase de un evento se **deriva de su fecha** y de las fechas de `estudio_fase` (no se guarda en cada fila: una sola fuente de verdad). Lo que congela los resultados es **cerrar la fase** (`estado = CERRADA` impide cambiar sus fechas y capturar fichas en ella). Servicio: `PeriodosEstudioService`.
- Los indicadores se pueden pedir con tres alcances: `GLOBAL` (todo el sistema, para la gestión diaria), `MUESTRA` (solo participantes incluidos) y `POR_PARTICIPANTE` (para Wilcoxon). La tesis usa `MUESTRA` y `POR_PARTICIPANTE`.

## 3. H1 — TPR, tiempo promedio de registro

### Qué se mide exactamente

La fórmula del Anexo 1 es `TPR = Σ TRC / NCR` con *TRC = tiempo de registro de citas* y *NCR = número de citas registradas*, mientras que la dimensión se llama "Registro de pacientes". Ambas lecturas se reconcilian así:

- **Evento principal (`REGISTRO_CITA`)**: el proceso de registrar la atención de un paciente (buscarlo/identificarlo, validar o actualizar sus datos y dejar la cita registrada). Ocurre **varias veces por paciente**, en ambas fases, y por eso permite pares pretest/postest para Wilcoxon. Es el que coincide con la fórmula.
- **Eventos complementarios**: `REGISTRO_PACIENTE` (alta de la ficha, ocurre una sola vez por paciente → útil como descriptivo, no pareable) y `ACTUALIZACION_PACIENTE`.

> Decisión a confirmar con el asesor: si la tesis define TPR sobre el alta del paciente, no habrá pares por paciente (cada paciente se registra una vez). Recomendación: mantener la fórmula sobre citas registradas y aclarar en el texto que "registro de pacientes" abarca el registro de su atención. Cualquier cambio aquí debe reflejarse en el Anexo 1 y en la matriz de consistencia.

### Cómo se mide (postest, automático)

Patrón **sesión de medición**, sellado por el servidor:

1. Al abrir el formulario (intranet: "Nueva cita", "Nuevo paciente"; portal: "Completar perfil"; chatbot: inicio de una consulta de agendamiento) el frontend llama `POST /api/v1/mediciones/registro` con `{tipo, canal}`. El backend crea la fila con `inicio = now()` y devuelve `medicionId`.
2. El request de guardado (`POST /citas`, `POST /pacientes`) incluye `medicionId`. El caso de uso, **en la misma transacción** que persiste la cita/paciente, cierra la medición: `fin = now()`, `entidad_id`, `paciente_id`, `usuario_id` del operador.
3. Una medición sin cerrar después de 60 min (configurable) se marca `ABANDONADA` por un job y **no entra** al promedio (sí se reporta como dato descriptivo: tasa de abandono de formularios).
4. Validaciones: la medición pertenece al mismo usuario que guarda; no se reutiliza; si `fin - inicio` < 5 s se marca `SOSPECHOSA` para revisión (no se descarta automáticamente).
5. Eliminar `tiempoRegistroSegundos` de los DTO y de los formularios Angular (hoy se calcula con `Date.now()` en el navegador). La columna `paciente.tiempo_registro_segundos` queda obsoleta (comentada en V6, ya no se lee ni se escribe).

Canales: `INTRANET`, `PORTAL`, `CHATBOT_WEB`, `TELEGRAM`, `MANUAL` (pretest). Los registros autoservicio (chatbot/portal) se reportan por separado, porque miden el tiempo del paciente y no del personal; el TPR de la hipótesis usa por defecto `INTRANET` + `MANUAL` (comparación justa: personal antes vs. personal después). Hacer el filtro de canal parametrizable en el endpoint.

### Cómo se mide (pretest, manual)

Pantalla "Ficha de registro de tiempos — Pretest" (rol `INVESTIGADOR`): participante, fecha, hora inicio, hora fin, tipo de registro, observación. Se guarda en la **misma tabla** con `canal = MANUAL`, `fase = PRETEST`, `capturado_por`. El cálculo es idéntico para ambas fases.

### Cálculo

```
TPR_min = AVG(duracion_segundos) / 60
          sobre medicion_registro
          donde estado = 'COMPLETADA' AND tipo = :tipo AND inicio dentro del periodo de la fase
            AND canal IN (:canales) AND [paciente en muestra]
```
Por participante: el mismo promedio agrupado por `paciente_id`.

## 4. H2 — TNS, tasa de ausentismo

### Qué se mide exactamente

`TNS = NI / (NI + NCC) × 100`

- `NI` (inasistencias): citas con desenlace `NO_ASISTIO` — el paciente no acudió **sin previo aviso**.
- `NCC` (citas cumplidas): citas con desenlace `ATENDIDA`.
- **No entran** al denominador: `CANCELADA` (hubo aviso), (una cita reprogramada es la misma cita: cuenta una vez, con su desenlace final; `veces_reprogramada` guarda cuántas veces se movió y no se puede reprogramar una cita en estado final), citas futuras, citas `PROGRAMADA`/`CONFIRMADA` sin desenlace registrado.
- El periodo se filtra por **fecha de la cita**, no por fecha de creación.

> El código actual calcula `NO_ASISTIO / total de citas del periodo`; ese denominador incluye canceladas y futuras y subestima el ausentismo. Corregir en `IndicadoresRepositoryAdapter`.

### Cómo se registra el desenlace

- Recepción marca `ATENDIDA` (check-in) o `NO_ASISTIO` desde la agenda del día (intranet). Guardar `fecha_hora_desenlace` y `desenlace_registrado_por`.
- Job diario (con ShedLock) a las 23:00: citas del día aún `PROGRAMADA`/`CONFIRMADA` → bandeja "Citas pendientes de cierre". A las 48 h sin acción, se cierran como `NO_ASISTIO` con `cierre_automatico = true` (corregible con evento de corrección auditado). Así nunca queda un hueco que sesgue el indicador.
- Cancelación tardía: si la fundación quiere tratar como inasistencia una cancelación con menos de N horas de anticipación, que sea un parámetro (`ausentismo.cancelacion-tardia-horas`, por defecto desactivado) y que se reporte aparte. No cambiar la definición en silencio.

### Pretest

- Importación de las hojas de cálculo históricas (CSV/XLSX → `cita` con `origen = IMPORTACION_PRETEST`) o captura en la "Ficha de registro de ausentismo — Pretest" (participante, fecha de cita, asistió S/N).
- Las citas importadas no disparan recordatorios ni eventos de negocio.

### Métricas de apoyo (explican el *por qué* bajó el TNS)

No forman parte de la hipótesis, pero sostienen la discusión: % de citas con recordatorio entregado, % confirmadas por Telegram, TNS de citas con vs. sin recordatorio, tiempo entre agendamiento y cita (McMullen y Netland).

## 5. H3 — NCA, nivel de consultas atendidas

### Qué es una "consulta"

Una consulta es **una necesidad del usuario**, no un mensaje. Una conversación puede contener varias consultas y una consulta puede tomar varios mensajes.

- Se **abre** con el primer mensaje de una sesión (web o Telegram) o cuando Gemini detecta una intención accionable distinta a la de la consulta abierta.
- Se **cierra** con uno de estos resultados:

| Resultado | Cuándo |
|---|---|
| `RESUELTA_BOT` | La acción del catálogo se ejecutó con éxito (agendar, reprogramar, cancelar, confirmar, consultar citas) **o** una intención informativa recibió respuesta y el usuario la valoró 👍 o no repitió/reformuló la misma intención en los siguientes 10 min |
| `ESCALADA` | Intención `ESCALATE_TO_STAFF`, valoración 👎, error de negocio no recuperable o dos reformulaciones seguidas. Pasa a la **bandeja de consultas** del personal |
| `RESUELTA_PERSONAL` | Un trabajador atendió la consulta escalada y la marcó como resuelta |
| `NO_RESUELTA` | Escalada sin respuesta en 48 h, o cerrada por el personal como no resuelta, o abandonada sin resolución en una intención accionable |

Cada consulta guarda: `canal` (`CHATBOT_WEB`, `TELEGRAM`, `WHATSAPP`, `LLAMADA`, `PRESENCIAL`), `intencion`, `paciente_id` (si se identificó), `sesion_id`, `abierta_en`, `cerrada_en`, `resultado`, `resuelta_por_usuario_id`, `valoracion` (1/-1/null), `tiempo_primera_respuesta_ms` (la fase se deriva de `abierta_en`).

### Cálculo

```
NCA      = (RESUELTA_BOT + RESUELTA_PERSONAL) / total consultas cerradas × 100   ← indicador de la hipótesis
NCA_auto = RESUELTA_BOT / total consultas cerradas × 100                          ← aporte específico del chatbot
```

Usar el NCA total para la hipótesis mantiene la comparación justa con el pretest (donde todo lo resolvía el personal) y el `NCA_auto` evidencia la automatización. Reportar ambos y el tiempo medio de primera respuesta. Las consultas abiertas al cierre del periodo se excluyen del denominador hasta cerrarse.

### Pretest

"Ficha de registro del sistema — Pretest": fecha, medio (WhatsApp/Llamada/Presencial), participante (si aplica), tipo de consulta, resuelta S/N. Se guarda en `consulta` con `fase = PRETEST`, `canal` correspondiente y `capturado_por`.

### Implementación en el chatbot actual

`ChatbotOrquestadorUseCase.procesar` hoy guarda cada mensaje en `conversacion_chatbot`. Añadir un `GestorConsultaService` que, en cada turno, decida abrir/continuar/cerrar la consulta en función de la intención y del resultado de `ejecutarAccion`. Para eso `ejecutarAccion` debe devolver un resultado tipado (`RespuestaChatbot(texto, ResultadoAccion)`: `EXITO`, `INFORMATIVA`, `REQUIERE_SESION`, `ERROR_NEGOCIO`, `ESCALAR`) en lugar de un `String`. Añadir botones 👍/👎 en el widget y en Telegram (`POST /chatbot/consultas/{id}/valoracion`).

## 6. Modelo de datos (implementado en `V6__modulo_estudio_indicadores.sql`)

La migración real es la fuente de verdad; léela antes de cambiar el modelo. Resumen:

- `estudio_fase` (PRETEST/POSTEST, fechas, ABIERTA/CERRADA) y `participante_estudio` (código Pnn, consentimiento, exclusión con motivo).
- `medicion_registro`: `tipo`, `canal`, `estado` (EN_CURSO/COMPLETADA/ABANDONADA/ANULADA), `sospechosa` (< 5 s), `inicio`, `fin`, `duracion_segundos` **generada** por PostgreSQL. Trigger `proteger_medicion_registro`: sin DELETE; solo se puede cerrar una EN_CURSO o anular una COMPLETADA.
- `consulta`: resultado null = abierta; ESCALADA espera al personal; trigger `proteger_consulta` (sin DELETE, fecha y canal inmutables, cerradas solo se anulan).
- `correccion_medicion`: toda anulación deja motivo, usuario y valores.
- `cita`: `origen` (INTRANET/PORTAL/CHATBOT_WEB/TELEGRAM/CAPTURA_PRETEST), `fecha_creacion`, `fecha_hora_desenlace`, `desenlace_registrado_por`, `cierre_automatico`, `veces_reprogramada`; `medico_id` admite null solo en CAPTURA_PRETEST.
- Tablas nuevas siempre con `TIMESTAMPTZ`; V7 agrega `turnos`, `ultima_actividad_en` y `nota_resolucion` a `consulta`, y la tabla `pregunta_frecuente`; la siguiente migración libre es **V8**.

## 7. Cálculo, endpoints y exportación para SPSS

Dominio: `domain/estudio/` con `FaseEstudio`, `ParticipanteEstudio`, `IndicadoresEstudio` (record con TPR, TNS, NCA, NCA_auto, n de cada uno) y un puerto `IndicadoresEstudioRepositoryPort`. Las fórmulas viven en el **dominio** (métodos estáticos puros, fáciles de testear: `Indicador.tns(ni, ncc)`), y el adaptador JPA solo aporta los conteos.

Endpoints (`/api/v1/estudio`, roles `ADMIN`, `INVESTIGADOR`):

| Método | Ruta | Uso |
|---|---|---|
| GET/PUT | `/fases` | Configurar periodos pretest/postest |
| GET/POST/PATCH | `/participantes` | Muestra, consentimiento, exclusiones |
| POST | `/mediciones/manual`, `/consultas/manual`, `/asistencias/manual` | Fichas del pretest |
| POST | `/importaciones/citas` | Importar hoja de cálculo histórica (CSV/XLSX con validación por fila) |
| GET | `/indicadores?fase=&alcance=` | TPR, TNS, NCA, NCA_auto con su n |
| GET | `/indicadores/comparativo` | Pretest vs postest + diferencia + % de mejora |
| GET | `/indicadores/pareado` | Tabla pareada por participante |
| GET | `/indicadores/wilcoxon` | Wilcoxon preliminar por indicador |
| GET | `/exportaciones/spss.xlsx` | Libro para SPSS |
| GET | `/exportaciones/fichas.xlsx?fase=&alcance=` | Las 3 fichas del Anexo 2 llenas, por fase |

Las exportaciones (implementadas en la Fase 6) son anónimas (solo `Pnn`, sin nombres, documentos ni textos libres como `resumen` u `observacion`), van con `Cache-Control: no-store` y cada descarga se audita (`ESTUDIO_EXPORTACION_SPSS` / `_FICHAS`).

Libro `spss.xlsx` (`GeneradorExportacionSpss`, Apache POI), en este orden:
- `pareado`: una fila por participante incluido → `codigo, tpr_pre, tpr_post, tns_pre, tns_post, nca_pre, nca_post` (nombres válidos en SPSS). Celda **vacía** (perdido del sistema), nunca 0, cuando no hay datos en una fase. Es la hoja que se abre en SPSS.
- `detalle`: formato largo participante × fase con los conteos detrás de cada porcentaje (`registros`, `inasistencias`, `citas_cumplidas`, `consultas_cerradas`, `consultas_resueltas_bot`…).
- `resumen`: comparativo de la muestra + Wilcoxon preliminar por indicador, con notas de convención.
- `diccionario`: etiqueta, tipo, medida y definición de cada variable. `metadatos`: periodos, filtros de TPR, fecha de generación.

Libro `fichas.xlsx` (`GeneradorExportacionFichas`): hojas `tiempos`, `asistencias`, `consultas` fila por fila y un `resumen` que pone el valor del sistema junto al mismo indicador **recalculado con fórmulas de Excel** sobre las filas. Las filas salen de `RegistrosEstudioRepositoryPort`, que implementa el mismo `IndicadoresEstudioJdbcAdapter` reutilizando los fragmentos `ORIGEN_TPR/TNS/NCA` de los conteos: si cambias qué cuenta para un indicador, cámbialo ahí y ambos se mantienen alineados (lo cubre un test de integración).

Wilcoxon preliminar: `domain/estudio/PruebaWilcoxon` (Java puro) reproduce las convenciones de SPSS — diferencia post − pre, descarta diferencias cero, rangos promedio, corrección de la varianza por empates, Z con el menor total de rangos y sin corrección por continuidad, p asintótica bilateral y p exacta condicional para n ≤ 50. Validado contra scipy en `PruebaWilcoxonTest`. Cada indicador usa solo los pares completos (`AnalisisPareado`). Es una **vista previa**: el análisis oficial (Shapiro-Wilk + Wilcoxon) se hace en SPSS. La intranet muestra siempre el n efectivo y advierte que con n < 10 la p asintótica es poco fiable.

## 8. Casos borde y tests obligatorios

- División por cero → devolver `null` ("sin datos"), no 0 % (0 % de ausentismo es un resultado, "sin datos" es otro).
- Cita reprogramada dos veces → solo la última cuenta para TNS.
- Cita cancelada → fuera del TNS; cita importada del pretest → no genera recordatorios.
- Medición abierta y nunca cerrada → `ABANDONADA`, fuera del TPR.
- Medición cerrada por otro usuario o reutilizada → rechazo (`ValidacionDeNegocioException`).
- Consulta de visitante anónimo → cuenta para NCA global, no para el pareado.
- Consulta abierta al final de la fase → excluida hasta cerrarse.
- Paciente excluido del estudio a mitad del postest → fuera de `MUESTRA` y del pareado, pero sus datos no se borran.
- Evento justo en el límite de fecha entre fases → la fase se decide por la fecha del evento con zona `America/Lima`.
- Usar `TIMESTAMPTZ` y `Clock` inyectable en los casos de uso para poder testear el tiempo.

Cada fórmula: test unitario de dominio. Cada consulta SQL de conteo: test de integración con Testcontainers sembrando un escenario conocido (p. ej., 10 citas: 6 atendidas, 3 no asistió, 1 cancelada → TNS = 33,3 %).

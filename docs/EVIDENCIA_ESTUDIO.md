# Evidencia del estudio

Registro de verificaciones de solo lectura sobre los datos del sistema que
alimentan los indicadores de la tesis. Ninguna de estas verificaciones
modifica datos: cada una indica la consulta usada y su resultado.

## 2026-10-03 · Saludos registrados como consultas (setiembre de 2026)

**Contexto.** Desde el 3 de octubre de 2026 el backend no cuenta como consulta
los saludos, agradecimientos ni los mensajes sin una solicitud concreta
(intención `GREETING`), ni los mensajes ajenos a la fundación (`OUT_OF_SCOPE`):
se responden, pero no entran al NCA. Antes de ese cambio un saludo podía abrir
una consulta. Se verificó si el periodo del 2 al 30 de setiembre de 2026 tiene
consultas que en realidad sean saludos, porque se habrían calculado con otra
regla.

**Base revisada.** PostgreSQL del despliegue local (`oncocare-postgres-1`,
base `oncologia`), revisada el 3 de octubre de 2026.

**Consultas usadas (solo lectura).**

```sql
-- Consultas abiertas entre el 2 y el 30 de setiembre (hora de Lima)
SELECT count(*) FROM consulta
WHERE abierta_en >= '2026-09-02 00:00-05' AND abierta_en < '2026-10-01 00:00-05';

-- Turnos del chatbot en el mismo periodo
SELECT count(*) FROM conversacion_chatbot
WHERE fecha >= '2026-09-02 00:00-05' AND fecha < '2026-10-01 00:00-05';

-- Consultas cuyo resumen es solo un saludo o agradecimiento (todo el historial)
SELECT count(*) FROM consulta
WHERE lower(coalesce(resumen, '')) ~ '^\s*(hola|buen[oa]s|gracias|ok|ya|chau|adios|\?+|\.+)\s*[!.?]*\s*$';
```

**Resultado.**

| Verificación | Resultado |
|---|---|
| Consultas del 2 al 30 de setiembre | 0 |
| Turnos del chatbot del 2 al 30 de setiembre | 0 |
| Consultas que son solo un saludo (todo el historial) | 0 |
| Primera y última consulta registradas | 03/10/2026 – 03/10/2026 (8 consultas, todas de pruebas del sistema) |

**Conclusión.** En esta base no hay consultas de setiembre: la primera es del
3 de octubre de 2026, el mismo día del cambio de regla. Por lo tanto no hay
consultas de setiembre calculadas con la regla anterior. Si el postest de
setiembre se registró en otro servidor, esta verificación debe repetirse sobre
esa base con las mismas consultas.

**Para la metodología de la tesis.** "No se consideraron consultas los
saludos, agradecimientos ni mensajes sin una solicitud concreta, ni los
mensajes ajenos a la atención de la fundación."

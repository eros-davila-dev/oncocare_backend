-- Quita los DATOS DE DEMOSTRACION cargados con cargar-datos-demo.sql.
--
-- Los registros de tiempo y las consultas son de solo insercion (protegen
-- los datos del estudio): no se borran, se ANULAN, y los indicadores ya
-- excluyen lo anulado. Las citas y recordatorios de demostracion se borran y
-- los pacientes DEMO-NNN quedan inactivos (los registros anulados los
-- referencian).
--
-- Uso: docker exec -i oncocare-postgres-1 psql -U oncologia -d oncologia < quitar-datos-demo.sql

BEGIN;

UPDATE medicion_registro SET estado = 'ANULADA'
WHERE observacion = 'DATOS DE DEMOSTRACION' AND estado = 'COMPLETADA';

UPDATE consulta SET resultado = 'ANULADA'
WHERE observacion = 'DATOS DE DEMOSTRACION' AND resultado IS DISTINCT FROM 'ANULADA';

DELETE FROM recordatorio
WHERE cita_id IN (SELECT id FROM cita WHERE observaciones = 'DATOS DE DEMOSTRACION');

DELETE FROM cita WHERE observaciones = 'DATOS DE DEMOSTRACION';

-- Tratamientos de cargar-modulos-demo.sql
DELETE FROM ciclo_tratamiento WHERE observaciones = 'DATOS DE DEMOSTRACION';

UPDATE paciente SET activo = false
WHERE documento_identidad LIKE 'DEMO-%' AND activo;

INSERT INTO auditoria_accion (usuario_id, accion, entidad_afectada, entidad_id, valores_previos, ip_origen, fecha, resultado)
VALUES (NULL, 'DATOS_DEMO_QUITADOS', 'SISTEMA', NULL, 'datos de demostracion anulados', 'script', now(), 'EXITO');

COMMIT;

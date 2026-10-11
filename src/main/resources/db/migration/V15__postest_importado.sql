-- Postest importado del instrumento (Excel) a nivel de sesion. Se guarda
-- separado de lo que mide el sistema y con su fuente explicita ("no medido
-- por este sistema"): sin pacientes ni eventos, solo los valores por sesion.
ALTER TABLE linea_base_sesion DROP CONSTRAINT linea_base_sesion_fase_check;
ALTER TABLE linea_base_sesion ADD CONSTRAINT linea_base_sesion_fase_check CHECK (fase IN ('PRETEST', 'POSTEST'));

-- Recordatorios de cita por correo, al paciente y (si lo autoriza) a su
-- referido: el contacto de emergencia, que ahora tambien registra su correo.
--
-- El correo del paciente y el del referido son obligatorios para registros
-- nuevos y al editar, pero la regla vive en el dominio (Paciente): aqui las
-- columnas quedan opcionales para no invalidar a los pacientes ya cargados.

ALTER TABLE paciente
    ADD COLUMN contacto_emergencia_email     VARCHAR(150),
    -- Ley 29733: enviar al referido datos de la cita requiere el consentimiento
    -- expreso del paciente; por defecto, no.
    ADD COLUMN contacto_recibe_recordatorios BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE recordatorio DROP CONSTRAINT recordatorio_canal_check;
ALTER TABLE recordatorio
    ADD CONSTRAINT recordatorio_canal_check CHECK (canal IN ('TELEGRAM', 'LLAMADA', 'CORREO'));

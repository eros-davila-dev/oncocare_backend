-- Vinculacion de Telegram sin enlace personal y para el acompanante.
--
-- 1. El referido (contacto de emergencia) puede vincular su propio Telegram y
--    recibir los recordatorios del paciente, si este lo autorizo
--    (contacto_recibe_recordatorios, V11). No es UNIQUE: un cuidador puede
--    acompanar a mas de un paciente.
ALTER TABLE paciente
    ADD COLUMN contacto_telegram_chat_id      BIGINT,
    ADD COLUMN contacto_telegram_vinculado_en TIMESTAMPTZ;

CREATE INDEX idx_paciente_contacto_telegram ON paciente (contacto_telegram_chat_id)
    WHERE contacto_telegram_chat_id IS NOT NULL;

-- Busqueda por telefono al compartir el numero en el bot: se comparan los
-- ultimos 9 digitos (formato movil peruano), sin espacios, guiones ni +51.
CREATE INDEX idx_paciente_telefono_norm ON paciente (RIGHT(REGEXP_REPLACE(telefono, '[^0-9]', '', 'g'), 9));
CREATE INDEX idx_paciente_contacto_telefono_norm
    ON paciente (RIGHT(REGEXP_REPLACE(contacto_emergencia_telefono, '[^0-9]', '', 'g'), 9));

-- 2. Recordatorios al referido: canal propio, para que cada destinatario
--    tenga su envio, sus reintentos y su respuesta.
ALTER TABLE recordatorio DROP CONSTRAINT recordatorio_canal_check;
ALTER TABLE recordatorio ALTER COLUMN canal TYPE VARCHAR(20);
ALTER TABLE recordatorio
    ADD CONSTRAINT recordatorio_canal_check CHECK (canal IN ('TELEGRAM', 'TELEGRAM_REFERIDO', 'LLAMADA', 'CORREO'));

-- 3. Vinculacion por telefono en dos pasos: el numero compartido encuentra
--    candidatos y el usuario confirma con los 3 ultimos digitos del DNI del
--    paciente (protege ante numeros reasignados por la operadora). Una fila
--    por chat, de vida corta; 3 intentos fallidos bloquean el chat 30 min.
CREATE TABLE telegram_vinculacion_pendiente (
    chat_id          BIGINT       PRIMARY KEY,
    titulares        VARCHAR(200) NOT NULL DEFAULT '',
    referidos        VARCHAR(200) NOT NULL DEFAULT '',
    intentos         INTEGER      NOT NULL DEFAULT 0,
    expira_en        TIMESTAMPTZ  NOT NULL,
    bloqueado_hasta  TIMESTAMPTZ
);

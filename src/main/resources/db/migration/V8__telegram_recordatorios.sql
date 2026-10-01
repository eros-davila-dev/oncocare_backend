-- Fase 4: recordatorios de citas por Telegram (indicador TNS) y chatbot por
-- Telegram (indicador NCA).
--
-- Un bot de Telegram solo puede escribir a quien inicio una conversacion con
-- el: por eso cada paciente vincula su chat con un token de un solo uso
-- (enlace t.me/<bot>?start=<token> o QR en recepcion).

ALTER TABLE paciente
    ADD COLUMN telegram_chat_id       BIGINT UNIQUE,
    ADD COLUMN telegram_vinculado_en  TIMESTAMPTZ,
    ADD COLUMN acepta_recordatorios   BOOLEAN NOT NULL DEFAULT TRUE;

-- Tokens de vinculacion. Tabla propia (no token_accion_cuenta) porque muchos
-- pacientes los registra el personal y no tienen cuenta de usuario.
CREATE TABLE token_vinculacion_telegram (
    id           BIGSERIAL    PRIMARY KEY,
    paciente_id  BIGINT       NOT NULL REFERENCES paciente (id),
    token_hash   VARCHAR(255) NOT NULL UNIQUE,
    expira_en    TIMESTAMPTZ  NOT NULL,
    usado_en     TIMESTAMPTZ,
    creado_en    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_token_vinculacion_paciente ON token_vinculacion_telegram (paciente_id);

-- Recordatorios programados. El backend decide que recordar y cuando; n8n
-- solo toma los pendientes, los envia y reporta el resultado.
-- canal LLAMADA: el paciente no tiene Telegram; recepcion lo llama.
CREATE TABLE recordatorio (
    id                   BIGSERIAL    PRIMARY KEY,
    cita_id              BIGINT       NOT NULL REFERENCES cita (id),
    tipo                 VARCHAR(10)  NOT NULL CHECK (tipo IN ('T72H', 'T24H', 'T2H')),
    canal                VARCHAR(15)  NOT NULL CHECK (canal IN ('TELEGRAM', 'LLAMADA')),
    programado_para      TIMESTAMPTZ  NOT NULL,
    estado               VARCHAR(12)  NOT NULL CHECK (estado IN ('PENDIENTE', 'EN_PROCESO', 'ENVIADO', 'FALLIDO', 'CANCELADO')),
    intentos             INTEGER      NOT NULL DEFAULT 0,
    mensaje_externo_id   VARCHAR(50),
    tomado_en            TIMESTAMPTZ,
    enviado_en           TIMESTAMPTZ,
    respuesta            VARCHAR(20)  CHECK (respuesta IN ('CONFIRMO', 'CANCELO', 'PIDIO_REPROGRAMAR')),
    respondido_en        TIMESTAMPTZ,
    error                TEXT,
    creado_en            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (cita_id, tipo, canal)
);

CREATE INDEX idx_recordatorio_pendiente ON recordatorio (estado, programado_para);
CREATE INDEX idx_recordatorio_cita ON recordatorio (cita_id);

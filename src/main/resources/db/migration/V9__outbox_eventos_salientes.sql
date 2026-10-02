-- Outbox transaccional hacia n8n (Fase 7).
--
-- Antes, los avisos a n8n (consulta escalada al personal, correos de
-- verificacion y recuperacion) se enviaban por HTTP dentro de la transaccion
-- del caso de uso: si n8n estaba caido el aviso se perdia, y si la
-- transaccion fallaba despues se avisaba de algo que no quedo guardado. Ahora
-- el caso de uso solo inserta aqui, en su misma transaccion, y un job entrega
-- con reintentos (entrega "al menos una vez").
--
-- payload se vacia al enviarse o al fallar definitivamente: puede contener un
-- correo o un enlace con token y no debe quedar guardado mas de lo necesario.
CREATE TABLE evento_saliente (
    id                  BIGSERIAL    PRIMARY KEY,
    destino             VARCHAR(200) NOT NULL,
    payload             JSONB        NOT NULL,
    estado              VARCHAR(10)  NOT NULL DEFAULT 'PENDIENTE'
        CHECK (estado IN ('PENDIENTE', 'EN_ENVIO', 'ENVIADO', 'FALLIDO')),
    intentos            SMALLINT     NOT NULL DEFAULT 0,
    creado_en           TIMESTAMPTZ  NOT NULL,
    proximo_intento_en  TIMESTAMPTZ  NOT NULL,
    tomado_en           TIMESTAMPTZ,
    enviado_en          TIMESTAMPTZ,
    ultimo_error        VARCHAR(500)
);

CREATE INDEX idx_evento_saliente_pendiente ON evento_saliente (proximo_intento_en) WHERE estado = 'PENDIENTE';
CREATE INDEX idx_evento_saliente_en_envio ON evento_saliente (tomado_en) WHERE estado = 'EN_ENVIO';
CREATE INDEX idx_evento_saliente_estado ON evento_saliente (estado, enviado_en);

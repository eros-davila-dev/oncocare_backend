-- Configuracion del asistente (Gemini) editable por el administrador desde la
-- intranet: clave de API y modelos en orden de preferencia. Si no hay fila,
-- o la fila no trae clave, se usan las variables de entorno (GEMINI_API_KEY,
-- GEMINI_MODEL, GEMINI_MODELS).
--
-- Una sola fila (id = 1). La clave se guarda cifrada con AES-GCM por el
-- backend (APP_CLAVE_CIFRADO): un volcado o respaldo de la base no la expone.
CREATE TABLE configuracion_gemini (
    id               SMALLINT      PRIMARY KEY DEFAULT 1 CHECK (id = 1),
    api_key_cifrada  TEXT,
    modelos          VARCHAR(1000) NOT NULL,
    actualizado_por  BIGINT        REFERENCES usuario (id),
    actualizado_en   TIMESTAMPTZ   NOT NULL DEFAULT now()
);

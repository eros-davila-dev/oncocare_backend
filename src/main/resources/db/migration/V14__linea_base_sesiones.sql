-- Linea base del estudio a nivel de sesion (tesis v8, opcion B): el pretest
-- se recolecto a mano antes del sistema y la fundacion lo valido. Se guarda
-- un valor por sesion e indicador, con su fuente, sin crear pacientes ni
-- eventos: asi nunca se confunde con lo que el sistema midio.
CREATE TABLE linea_base_sesion (
    id          BIGSERIAL    PRIMARY KEY,
    fase        VARCHAR(10)  NOT NULL CHECK (fase = 'PRETEST'),
    numero      INTEGER      NOT NULL CHECK (numero > 0),
    fecha       DATE         NOT NULL,
    tpr_min     NUMERIC(8, 2) CHECK (tpr_min IS NULL OR tpr_min >= 0),
    ta_pct      NUMERIC(6, 2) CHECK (ta_pct IS NULL OR ta_pct BETWEEN 0 AND 100),
    nca_pct     NUMERIC(6, 2) CHECK (nca_pct IS NULL OR nca_pct BETWEEN 0 AND 100),
    fuente      VARCHAR(200) NOT NULL,
    cargado_por BIGINT       REFERENCES usuario (id) ON DELETE SET NULL,
    cargado_en  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (fase, fecha),
    UNIQUE (fase, numero)
);

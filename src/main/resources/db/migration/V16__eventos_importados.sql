-- Eventos por paciente del instrumento (hojas TPR_, Ausentismo_ y Consultas_
-- PRE/POST): pretest de las fichas manuales y postest exportado del servidor
-- de la fundacion. El paciente va solo con su codigo anonimo (PAC-NNN): no se
-- crean pacientes ni usuarios, y nada se mezcla con lo que mide esta
-- instalacion.
CREATE TABLE evento_importado (
    id                  BIGSERIAL    PRIMARY KEY,
    fase                VARCHAR(10)  NOT NULL CHECK (fase IN ('PRETEST', 'POSTEST')),
    tipo                VARCHAR(10)  NOT NULL CHECK (tipo IN ('REGISTRO', 'CITA', 'CONSULTA')),
    numero              INTEGER      NOT NULL,
    fecha               DATE         NOT NULL,
    codigo_paciente     VARCHAR(20)  NOT NULL,
    hora_inicio         TIME,
    hora_fin            TIME,
    minutos             NUMERIC(8, 2),
    modalidad           VARCHAR(40),
    estado              VARCHAR(30),
    motivo              VARCHAR(120),
    recordatorio        BOOLEAN,
    categoria           VARCHAR(60),
    canal               VARCHAR(40),
    resuelta            BOOLEAN,
    derivada            BOOLEAN,
    tiempo_respuesta    NUMERIC(8, 2),
    control_calidad     VARCHAR(80),
    fuente              VARCHAR(200) NOT NULL,
    cargado_por         BIGINT       REFERENCES usuario (id) ON DELETE SET NULL,
    cargado_en          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (fase, tipo, numero)
);
CREATE INDEX idx_evento_importado_tipo ON evento_importado (tipo, fase, fecha);

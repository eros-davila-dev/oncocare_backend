-- Esquema inicial del sistema de gestion de pacientes oncologicos (seccion 8)

CREATE TABLE usuario (
    id              BIGSERIAL PRIMARY KEY,
    nombres         VARCHAR(150) NOT NULL,
    email           VARCHAR(150) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    rol             VARCHAR(20)  NOT NULL,
    especialidad    VARCHAR(30),
    activo          BOOLEAN      NOT NULL DEFAULT TRUE,
    fecha_creacion  TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE TABLE paciente (
    id                              BIGSERIAL PRIMARY KEY,
    nombres                         VARCHAR(150) NOT NULL,
    apellidos                       VARCHAR(150) NOT NULL,
    documento_identidad             VARCHAR(20)  NOT NULL UNIQUE,
    fecha_nacimiento                DATE         NOT NULL,
    telefono                        VARCHAR(20),
    email                           VARCHAR(150),
    direccion                       VARCHAR(200),
    tipo_cancer                     VARCHAR(100),
    estadio_clinico                 VARCHAR(60),
    fecha_diagnostico               DATE,
    medico_tratante_id              BIGINT REFERENCES usuario (id),
    convenio_seguro                 VARCHAR(20),
    contacto_emergencia_nombre      VARCHAR(150),
    contacto_emergencia_telefono    VARCHAR(20),
    activo                          BOOLEAN      NOT NULL DEFAULT TRUE,
    fecha_registro                  TIMESTAMP    NOT NULL DEFAULT now(),
    tiempo_registro_segundos        INTEGER
);

CREATE TABLE cita (
    id              BIGSERIAL PRIMARY KEY,
    paciente_id     BIGINT       NOT NULL REFERENCES paciente (id),
    medico_id       BIGINT       NOT NULL REFERENCES usuario (id),
    fecha           DATE         NOT NULL,
    hora            TIME         NOT NULL,
    tipo_consulta   VARCHAR(100),
    estado          VARCHAR(20)  NOT NULL,
    observaciones   TEXT
);

CREATE INDEX idx_cita_medico_fecha_hora ON cita (medico_id, fecha, hora);
CREATE INDEX idx_cita_paciente ON cita (paciente_id);
CREATE INDEX idx_cita_fecha ON cita (fecha);

CREATE TABLE ciclo_tratamiento (
    id                          BIGSERIAL PRIMARY KEY,
    paciente_id                 BIGINT      NOT NULL REFERENCES paciente (id),
    tipo_tratamiento            VARCHAR(25) NOT NULL,
    numero_sesion               INTEGER     NOT NULL,
    total_sesiones_esquema      INTEGER     NOT NULL,
    fecha_sesion                DATE        NOT NULL,
    medico_responsable_id       BIGINT      NOT NULL REFERENCES usuario (id),
    estado                      VARCHAR(20) NOT NULL,
    observaciones               TEXT
);

CREATE INDEX idx_ciclo_tratamiento_paciente ON ciclo_tratamiento (paciente_id);

CREATE TABLE documento_paciente (
    id              BIGSERIAL PRIMARY KEY,
    paciente_id     BIGINT       NOT NULL REFERENCES paciente (id),
    tipo_documento  VARCHAR(30)  NOT NULL,
    url_archivo     VARCHAR(500) NOT NULL,
    fecha_carga     TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_documento_paciente ON documento_paciente (paciente_id);

CREATE TABLE notificacion (
    id              BIGSERIAL PRIMARY KEY,
    cita_id         BIGINT      NOT NULL REFERENCES cita (id),
    canal           VARCHAR(15) NOT NULL,
    estado_envio    VARCHAR(15) NOT NULL,
    fecha_envio     TIMESTAMP   NOT NULL DEFAULT now()
);

CREATE TABLE conversacion_chatbot (
    id                      BIGSERIAL PRIMARY KEY,
    paciente_id             BIGINT REFERENCES paciente (id),
    mensaje_usuario         TEXT        NOT NULL,
    respuesta_bot           TEXT,
    intencion_detectada     VARCHAR(60),
    fecha                   TIMESTAMP   NOT NULL DEFAULT now(),
    canal                   VARCHAR(20)
);

-- Tabla de auditoria: de solo insercion (seccion 13). Un trigger impide
-- UPDATE/DELETE a nivel de base de datos, independientemente del rol que
-- se conecte, para que la inmutabilidad no dependa unicamente de la capa de
-- aplicacion.
CREATE TABLE auditoria_accion (
    id                  BIGSERIAL PRIMARY KEY,
    usuario_id          BIGINT REFERENCES usuario (id) ON DELETE SET NULL,
    accion              VARCHAR(60) NOT NULL,
    entidad_afectada    VARCHAR(40) NOT NULL,
    entidad_id          VARCHAR(60),
    valores_previos     TEXT,
    valores_nuevos      TEXT,
    ip_origen           VARCHAR(45),
    fecha               TIMESTAMP   NOT NULL,
    resultado           VARCHAR(10) NOT NULL
);

CREATE INDEX idx_auditoria_usuario ON auditoria_accion (usuario_id);
CREATE INDEX idx_auditoria_entidad ON auditoria_accion (entidad_afectada);
CREATE INDEX idx_auditoria_fecha ON auditoria_accion (fecha);

CREATE OR REPLACE FUNCTION impedir_modificacion_auditoria()
    RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'La tabla auditoria_accion es de solo insercion: % no esta permitido', TG_OP;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_auditoria_solo_insercion
    BEFORE UPDATE OR DELETE ON auditoria_accion
    FOR EACH ROW EXECUTE FUNCTION impedir_modificacion_auditoria();

CREATE TABLE dispositivo_externo (
    id                  BIGSERIAL PRIMARY KEY,
    nombre              VARCHAR(150) NOT NULL,
    tipo                VARCHAR(30)  NOT NULL,
    protocolo           VARCHAR(10)  NOT NULL,
    estado_conexion     VARCHAR(20)  NOT NULL,
    credencial_hash     VARCHAR(255)
);

CREATE TABLE lectura_dispositivo (
    id                      BIGSERIAL PRIMARY KEY,
    dispositivo_id          BIGINT      NOT NULL REFERENCES dispositivo_externo (id),
    paciente_id             BIGINT REFERENCES paciente (id),
    ciclo_tratamiento_id    BIGINT REFERENCES ciclo_tratamiento (id),
    tipo_dato               VARCHAR(60) NOT NULL,
    valor                   VARCHAR(255) NOT NULL,
    fecha_lectura           TIMESTAMP   NOT NULL
);

CREATE INDEX idx_lectura_dispositivo_paciente ON lectura_dispositivo (paciente_id);
CREATE INDEX idx_lectura_dispositivo_ciclo ON lectura_dispositivo (ciclo_tratamiento_id);

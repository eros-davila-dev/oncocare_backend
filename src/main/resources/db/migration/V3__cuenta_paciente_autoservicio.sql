-- Portal de autoservicio para pacientes (secciones 6-9). "usuario" pasa a ser
-- la tabla de CUENTA para todos los roles, incluido el nuevo rol PACIENTE;
-- "paciente" sigue siendo el registro clinico. usuario_id vincula el
-- registro clinico con su cuenta de autoservicio cuando existe (queda nulo
-- para los pacientes dados de alta unicamente por el staff, sin cuenta
-- propia).

ALTER TABLE usuario
    ADD COLUMN estado_cuenta     VARCHAR(25) NOT NULL DEFAULT 'ACTIVA',
    ADD COLUMN intentos_fallidos INTEGER     NOT NULL DEFAULT 0,
    ADD COLUMN bloqueado_hasta   TIMESTAMP;

ALTER TABLE paciente
    ADD COLUMN usuario_id BIGINT UNIQUE REFERENCES usuario (id);

-- Evita duplicar pacientes por correo (seccion 9), sin invalidar los
-- registros existentes con email nulo (un indice unico parcial permite
-- multiples NULL sin conflicto).
CREATE UNIQUE INDEX idx_paciente_email_unico ON paciente (email) WHERE email IS NOT NULL;

CREATE TABLE token_accion_cuenta (
    id              BIGSERIAL    PRIMARY KEY,
    usuario_id      BIGINT       NOT NULL REFERENCES usuario (id),
    tipo            VARCHAR(20)  NOT NULL,
    token_hash      VARCHAR(255) NOT NULL UNIQUE,
    expira_en       TIMESTAMP    NOT NULL,
    usado_en        TIMESTAMP,
    creado_en       TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_token_accion_cuenta_usuario ON token_accion_cuenta (usuario_id);

-- Refresh tokens persistidos solo por su hash (seccion 12): habilita logout
-- real, rotacion en cada /auth/refresh y revocacion masiva al restablecer la
-- contrasena. El access token permanece stateless (vida corta).
CREATE TABLE sesion_refresh_token (
    id              BIGSERIAL    PRIMARY KEY,
    usuario_id      BIGINT       NOT NULL REFERENCES usuario (id),
    token_hash      VARCHAR(255) NOT NULL UNIQUE,
    expira_en       TIMESTAMP    NOT NULL,
    revocado_en     TIMESTAMP,
    ip_origen       VARCHAR(45),
    creado_en       TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_sesion_refresh_token_usuario ON sesion_refresh_token (usuario_id);

-- Modulo de estudio (Fase 1 del plan): todo lo necesario para medir los tres
-- indicadores de la tesis de forma automatica, auditable y pareada por
-- paciente (pretest vs postest, prueba de Wilcoxon).
--
--   H1  TPR = suma(TRC) / NCR            -> medicion_registro
--   H2  TNS = NI / (NI + NCC) x 100      -> cita (desenlace ATENDIDA / NO_ASISTIO)
--   H3  NCA = CA / TCR x 100             -> consulta
--
-- Regla general: la fase (PRETEST/POSTEST) de un evento la definen sus fechas
-- y las fechas configuradas en estudio_fase; no se guarda en cada fila. Una
-- fase CERRADA ya no puede cambiar sus fechas, lo que congela los resultados.

CREATE TABLE estudio_fase (
    id              BIGSERIAL   PRIMARY KEY,
    fase            VARCHAR(10) NOT NULL UNIQUE CHECK (fase IN ('PRETEST', 'POSTEST')),
    fecha_inicio    DATE        NOT NULL,
    fecha_fin       DATE        NOT NULL,
    estado          VARCHAR(10) NOT NULL DEFAULT 'ABIERTA' CHECK (estado IN ('ABIERTA', 'CERRADA')),
    actualizado_en  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (fecha_fin >= fecha_inicio)
);

-- Muestra del estudio (30 de 32 pacientes). codigo (P01, P02...) es el unico
-- identificador que sale en las exportaciones (Ley 29733).
CREATE TABLE participante_estudio (
    id                    BIGSERIAL   PRIMARY KEY,
    paciente_id           BIGINT      NOT NULL UNIQUE REFERENCES paciente (id),
    codigo                VARCHAR(10) NOT NULL UNIQUE,
    fecha_consentimiento  DATE        NOT NULL,
    incluido              BOOLEAN     NOT NULL DEFAULT TRUE,
    motivo_exclusion      VARCHAR(30),
    observacion           TEXT,
    fecha_inclusion       TIMESTAMPTZ NOT NULL DEFAULT now(),
    fecha_exclusion       TIMESTAMPTZ,
    CHECK (incluido OR motivo_exclusion IS NOT NULL)
);

-- Sesiones de medicion del tiempo de registro (TPR). inicio y fin los sella
-- SIEMPRE el servidor (o el investigador, para las fichas del pretest con
-- canal MANUAL); el navegador nunca envia una duracion.
CREATE TABLE medicion_registro (
    id                 BIGSERIAL   PRIMARY KEY,
    tipo               VARCHAR(30) NOT NULL CHECK (tipo IN ('REGISTRO_CITA', 'REGISTRO_PACIENTE', 'ACTUALIZACION_PACIENTE')),
    canal              VARCHAR(20) NOT NULL CHECK (canal IN ('INTRANET', 'PORTAL', 'CHATBOT_WEB', 'TELEGRAM', 'MANUAL')),
    estado             VARCHAR(15) NOT NULL CHECK (estado IN ('EN_CURSO', 'COMPLETADA', 'ABANDONADA', 'ANULADA')),
    sospechosa         BOOLEAN     NOT NULL DEFAULT FALSE,
    usuario_id         BIGINT      REFERENCES usuario (id),
    paciente_id        BIGINT      REFERENCES paciente (id),
    entidad_id         BIGINT,
    inicio             TIMESTAMPTZ NOT NULL,
    fin                TIMESTAMPTZ,
    duracion_segundos  INTEGER GENERATED ALWAYS AS ((EXTRACT(EPOCH FROM (fin - inicio)))::INTEGER) STORED,
    capturado_por      BIGINT      REFERENCES usuario (id),
    observacion        TEXT,
    CHECK (fin IS NULL OR fin >= inicio),
    CHECK (estado <> 'COMPLETADA' OR fin IS NOT NULL)
);

CREATE INDEX idx_medicion_registro_calculo ON medicion_registro (estado, tipo, canal, inicio);
CREATE INDEX idx_medicion_registro_paciente ON medicion_registro (paciente_id);
CREATE INDEX idx_medicion_registro_en_curso ON medicion_registro (inicio) WHERE estado = 'EN_CURSO';

-- Consultas (NCA): una consulta es UNA necesidad del usuario, no un mensaje.
-- resultado NULL = abierta; ESCALADA = esperando al personal. Solo las
-- consultas con resultado final (RESUELTA_BOT, RESUELTA_PERSONAL, NO_RESUELTA)
-- entran al denominador.
CREATE TABLE consulta (
    id                           BIGSERIAL    PRIMARY KEY,
    canal                        VARCHAR(20)  NOT NULL CHECK (canal IN ('CHATBOT_WEB', 'TELEGRAM', 'WHATSAPP', 'LLAMADA', 'PRESENCIAL')),
    sesion_id                    VARCHAR(100),
    paciente_id                  BIGINT       REFERENCES paciente (id),
    intencion                    VARCHAR(40),
    resumen                      VARCHAR(300),
    resultado                    VARCHAR(20)  CHECK (resultado IN ('RESUELTA_BOT', 'RESUELTA_PERSONAL', 'ESCALADA', 'NO_RESUELTA', 'ANULADA')),
    abierta_en                   TIMESTAMPTZ  NOT NULL,
    cerrada_en                   TIMESTAMPTZ,
    tiempo_primera_respuesta_ms  INTEGER,
    valoracion                   SMALLINT     CHECK (valoracion IN (-1, 1)),
    resuelta_por_usuario_id      BIGINT       REFERENCES usuario (id),
    capturado_por                BIGINT       REFERENCES usuario (id),
    observacion                  TEXT
);

CREATE INDEX idx_consulta_calculo ON consulta (resultado, abierta_en);
CREATE INDEX idx_consulta_sesion ON consulta (sesion_id);
CREATE INDEX idx_consulta_paciente ON consulta (paciente_id);
CREATE INDEX idx_consulta_escalada ON consulta (abierta_en) WHERE resultado = 'ESCALADA';

ALTER TABLE conversacion_chatbot
    ADD COLUMN consulta_id BIGINT REFERENCES consulta (id);

-- Correcciones: nunca se reescribe un dato de medicion en silencio; cada
-- ajuste deja motivo, usuario y valores antes/despues.
CREATE TABLE correccion_medicion (
    id            BIGSERIAL   PRIMARY KEY,
    entidad       VARCHAR(30) NOT NULL CHECK (entidad IN ('MEDICION_REGISTRO', 'CONSULTA', 'CITA_DESENLACE')),
    entidad_id    BIGINT      NOT NULL,
    campo         VARCHAR(40) NOT NULL,
    valor_previo  TEXT,
    valor_nuevo   TEXT,
    motivo        TEXT        NOT NULL,
    usuario_id    BIGINT      NOT NULL REFERENCES usuario (id),
    fecha         TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_correccion_medicion_entidad ON correccion_medicion (entidad, entidad_id);

-- Citas: trazabilidad del desenlace (TNS) y del origen del agendamiento.
-- Las citas del pretest capturadas por el investigador (origen
-- CAPTURA_PRETEST) pueden no tener medico registrado en las hojas de calculo
-- originales; cualquier otra cita lo sigue exigiendo.
ALTER TABLE cita
    ADD COLUMN origen                    VARCHAR(25) NOT NULL DEFAULT 'INTRANET'
        CHECK (origen IN ('INTRANET', 'PORTAL', 'CHATBOT_WEB', 'TELEGRAM', 'CAPTURA_PRETEST')),
    ADD COLUMN fecha_creacion            TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN fecha_hora_desenlace      TIMESTAMPTZ,
    ADD COLUMN desenlace_registrado_por  BIGINT      REFERENCES usuario (id),
    ADD COLUMN cierre_automatico         BOOLEAN     NOT NULL DEFAULT FALSE,
    ADD COLUMN veces_reprogramada        INTEGER     NOT NULL DEFAULT 0,
    ALTER COLUMN medico_id DROP NOT NULL;

ALTER TABLE cita
    ADD CONSTRAINT chk_cita_medico_requerido CHECK (medico_id IS NOT NULL OR origen = 'CAPTURA_PRETEST');

CREATE INDEX idx_cita_estado_fecha ON cita (estado, fecha);

-- El tiempo de registro que enviaba el navegador deja de usarse (no era un
-- "timestamp generado por el sistema"). Se conserva la columna para no perder
-- datos historicos, pero ya no se escribe ni se lee.
COMMENT ON COLUMN paciente.tiempo_registro_segundos IS
    'OBSOLETA desde V6: el TPR se mide en medicion_registro con timestamps del servidor';

-- Proteccion a nivel de base de datos (como auditoria_accion): ni el codigo
-- ni un usuario con acceso directo pueden borrar mediciones ni reescribir una
-- ya cerrada. Solo se permite cerrar una EN_CURSO o anular una COMPLETADA.
CREATE OR REPLACE FUNCTION proteger_medicion_registro()
    RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        RAISE EXCEPTION 'medicion_registro es de solo insercion: DELETE no esta permitido';
    END IF;
    IF OLD.estado = 'EN_CURSO' THEN
        RETURN NEW;
    END IF;
    IF OLD.estado = 'COMPLETADA' AND NEW.estado IN ('COMPLETADA', 'ANULADA')
       AND NEW.inicio = OLD.inicio AND NEW.fin = OLD.fin
       AND NEW.tipo = OLD.tipo AND NEW.canal = OLD.canal
       AND NEW.paciente_id IS NOT DISTINCT FROM OLD.paciente_id THEN
        RETURN NEW;
    END IF;
    IF NEW IS NOT DISTINCT FROM OLD THEN
        RETURN NEW;
    END IF;
    RAISE EXCEPTION 'No se puede modificar una medicion en estado %', OLD.estado;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_medicion_registro_proteccion
    BEFORE UPDATE OR DELETE ON medicion_registro
    FOR EACH ROW EXECUTE FUNCTION proteger_medicion_registro();

CREATE OR REPLACE FUNCTION proteger_consulta()
    RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        RAISE EXCEPTION 'consulta es de solo insercion: DELETE no esta permitido';
    END IF;
    IF NEW.abierta_en <> OLD.abierta_en OR NEW.canal <> OLD.canal THEN
        RAISE EXCEPTION 'No se puede modificar la fecha ni el canal de una consulta';
    END IF;
    IF OLD.resultado = 'ANULADA' AND NEW IS DISTINCT FROM OLD THEN
        RAISE EXCEPTION 'Una consulta anulada no puede modificarse';
    END IF;
    IF OLD.resultado IN ('RESUELTA_PERSONAL', 'NO_RESUELTA')
       AND NEW.resultado NOT IN (OLD.resultado, 'ANULADA') THEN
        RAISE EXCEPTION 'Una consulta cerrada como % solo puede anularse', OLD.resultado;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_consulta_proteccion
    BEFORE UPDATE OR DELETE ON consulta
    FOR EACH ROW EXECUTE FUNCTION proteger_consulta();

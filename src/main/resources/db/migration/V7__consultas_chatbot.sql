-- Fase 3: el chatbot orientado a consultas (indicador NCA).
--
-- Una consulta agrupa los turnos de conversacion de una misma necesidad.
-- turnos y ultima_actividad_en permiten decidir cuando una consulta abierta
-- se abandono (se cierra como NO_RESUELTA) y cuando el bot no logra
-- resolverla (se escala al personal).

ALTER TABLE consulta DISABLE TRIGGER trg_consulta_proteccion;

ALTER TABLE consulta
    ADD COLUMN turnos               INTEGER     NOT NULL DEFAULT 1,
    ADD COLUMN ultima_actividad_en  TIMESTAMPTZ,
    ADD COLUMN nota_resolucion      TEXT;

UPDATE consulta SET ultima_actividad_en = COALESCE(cerrada_en, abierta_en);

ALTER TABLE consulta
    ALTER COLUMN ultima_actividad_en SET DEFAULT now(),
    ALTER COLUMN ultima_actividad_en SET NOT NULL;

ALTER TABLE consulta ENABLE TRIGGER trg_consulta_proteccion;

CREATE INDEX idx_consulta_abierta_actividad ON consulta (ultima_actividad_en) WHERE resultado IS NULL;

-- Base de conocimiento institucional que el chatbot usa como UNICA fuente
-- para responder preguntas generales (horarios, requisitos, ubicacion...).
-- La administra la fundacion desde la intranet: el modelo nunca inventa
-- estos datos.
CREATE TABLE pregunta_frecuente (
    id              BIGSERIAL    PRIMARY KEY,
    pregunta        VARCHAR(200) NOT NULL,
    respuesta       TEXT         NOT NULL,
    categoria       VARCHAR(40)  NOT NULL DEFAULT 'GENERAL',
    orden           INTEGER      NOT NULL DEFAULT 0,
    activa          BOOLEAN      NOT NULL DEFAULT TRUE,
    actualizado_en  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_pregunta_frecuente_activa ON pregunta_frecuente (activa, orden);

-- Solo preguntas sobre el propio sistema (verdaderas por construccion). Los
-- datos de la fundacion (horarios, direccion, requisitos) los carga el
-- personal desde la intranet.
INSERT INTO pregunta_frecuente (pregunta, respuesta, categoria, orden) VALUES
('¿Cómo creo mi cuenta de paciente?',
 'Ingresa al portal, elige "Crear cuenta", registra tu correo y una contraseña, confirma tu correo y luego completa tu ficha con tu documento y datos de contacto.',
 'PORTAL', 1),
('¿Cómo veo, confirmo o cancelo mis citas?',
 'Con tu sesión iniciada entra a "Mis citas". Ahí puedes ver tus próximas citas, confirmarlas, reprogramarlas o cancelarlas. También puedes pedírmelo por este chat.',
 'CITAS', 2),
('¿Cómo recibo recordatorios de mis citas?',
 'Vincula tu Telegram desde "Mi perfil" o pide el código QR en recepción. Te avisaremos antes de cada cita y podrás confirmar con un botón.',
 'RECORDATORIOS', 3);

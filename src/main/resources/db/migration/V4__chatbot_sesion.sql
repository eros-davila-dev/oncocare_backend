-- Agrupa los mensajes de una misma conversacion del widget (seccion 41): el
-- backend usa este historial para darle contexto real a Gemini en cada turno,
-- en vez de depender solo de la memoria del modelo.
ALTER TABLE conversacion_chatbot
    ADD COLUMN sesion_id VARCHAR(100);

CREATE INDEX idx_conversacion_chatbot_sesion ON conversacion_chatbot (sesion_id, fecha);

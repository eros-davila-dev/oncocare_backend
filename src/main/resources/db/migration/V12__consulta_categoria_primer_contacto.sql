-- NCA segun la tesis v8: "consultas resueltas en el primer contacto y sin
-- derivacion / consultas recibidas", agrupadas por categoria.
--
-- categoria: la de la ficha de consultas (Anexo 2). FUERA_DE_ALCANCE no se
--   guarda como consulta (el orquestador no la registra); OTRO es una
--   consulta valida que no encaja en las demas.
-- derivada: la consulta paso al personal en algun momento (aunque luego se
--   resolviera). Una derivada nunca cuenta como resuelta en primer contacto.
-- reabierta: el usuario volvio a preguntar lo mismo tras la respuesta del bot
--   ("volver a comunicarse por el mismo motivo"): tampoco cuenta.
ALTER TABLE consulta
    ADD COLUMN categoria VARCHAR(30) CHECK (categoria IN ('CITAS', 'HORARIOS', 'INFORMACION_INSTITUCIONAL',
        'REQUISITOS', 'UBICACION', 'SEGUIMIENTO_ADMINISTRATIVO', 'OTRO')),
    ADD COLUMN derivada  BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN reabierta BOOLEAN NOT NULL DEFAULT FALSE;

-- Las ya escaladas o atendidas por el personal fueron derivadas. El trigger
-- de proteccion no deja tocar consultas cerradas por el personal: se apaga
-- solo para este relleno, como en V7.
ALTER TABLE consulta DISABLE TRIGGER trg_consulta_proteccion;
UPDATE consulta SET derivada = TRUE WHERE resultado IN ('ESCALADA', 'RESUELTA_PERSONAL') AND canal IN ('CHATBOT_WEB', 'TELEGRAM');
ALTER TABLE consulta ENABLE TRIGGER trg_consulta_proteccion;

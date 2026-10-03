-- Base de datos propia de n8n (workflows, credenciales cifradas, historial de
-- ejecuciones), separada de la base "oncologia" del sistema para que un
-- problema o una migracion de n8n nunca afecte los datos clinicos.
CREATE DATABASE n8n OWNER oncologia;

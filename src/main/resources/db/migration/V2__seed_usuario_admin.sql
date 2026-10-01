-- Usuario administrador inicial para el primer acceso al sistema.
-- Credenciales por defecto (cambiar inmediatamente en produccion):
--   email:    admin@threepartners.org
--   password: Admin123!
INSERT INTO usuario (nombres, email, password_hash, rol, activo, fecha_creacion)
VALUES (
    'Administrador del Sistema',
    'admin@threepartners.org',
    '$2b$10$FQqN0XiN8X5.1GQFJFcGXuzUVCMYym/aZu/RmkVF0cW6oCVHvKhwO',
    'ADMIN',
    TRUE,
    now()
);

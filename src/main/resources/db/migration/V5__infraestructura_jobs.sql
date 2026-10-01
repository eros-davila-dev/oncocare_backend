-- Candados de ShedLock (Fase 0 del plan): garantizan que cada job programado
-- (cierre de mediciones abandonadas, citas pendientes de cierre,
-- recordatorios) corra una sola vez aunque haya varias replicas del backend.
CREATE TABLE shedlock (
    name        VARCHAR(64)  NOT NULL PRIMARY KEY,
    lock_until  TIMESTAMP    NOT NULL,
    locked_at   TIMESTAMP    NOT NULL,
    locked_by   VARCHAR(255) NOT NULL
);

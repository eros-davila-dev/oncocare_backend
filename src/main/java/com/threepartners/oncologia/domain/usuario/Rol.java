package com.threepartners.oncologia.domain.usuario;

public enum Rol {
    ADMIN,
    MEDICO,
    RECEPCIONISTA,
    PACIENTE,
    /**
     * Responsable del estudio de tesis: configura fases pretest/postest,
     * gestiona la muestra, captura las fichas del pretest y exporta los datos.
     */
    INVESTIGADOR,
    /**
     * Cuenta tecnica para integraciones (n8n). Nunca la usa una persona.
     */
    SERVICIO
}

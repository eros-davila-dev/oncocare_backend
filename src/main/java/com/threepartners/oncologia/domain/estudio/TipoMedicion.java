package com.threepartners.oncologia.domain.estudio;

/**
 * REGISTRO_CITA es el evento del indicador TPR de la hipotesis (formula del
 * Anexo 1: tiempo de registro de citas / numero de citas registradas). Ocurre
 * varias veces por paciente y por eso admite pares pretest/postest para la
 * prueba de Wilcoxon. Los otros dos tipos se reportan como descriptivos.
 */
public enum TipoMedicion {
    REGISTRO_CITA,
    REGISTRO_PACIENTE,
    ACTUALIZACION_PACIENTE
}

package com.threepartners.oncologia.domain.estudio;

/**
 * Participante con los datos minimos del paciente para mostrarlo en la
 * intranet (nunca se exporta: la exportacion solo usa el codigo).
 */
public record ParticipanteResumen(
        ParticipanteEstudio participante,
        String nombreCompleto,
        String documentoIdentidad
) {
}

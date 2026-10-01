package com.threepartners.oncologia.domain.estudio;

import com.threepartners.oncologia.domain.shared.exception.ValidacionDeNegocioException;

import java.time.Instant;

/**
 * Rastro de cualquier ajuste a un dato que alimenta un indicador. Sin motivo
 * no hay correccion: el jurado de la tesis debe poder ver por que cambio un
 * dato.
 */
public record CorreccionMedicion(
        Long id,
        EntidadCorregida entidad,
        Long entidadId,
        String campo,
        String valorPrevio,
        String valorNuevo,
        String motivo,
        Long usuarioId,
        Instant fecha
) {

    public CorreccionMedicion {
        if (motivo == null || motivo.isBlank()) {
            throw new ValidacionDeNegocioException("Toda correccion de un dato del estudio requiere un motivo");
        }
    }

    public enum EntidadCorregida {
        MEDICION_REGISTRO,
        CONSULTA,
        CITA_DESENLACE
    }
}

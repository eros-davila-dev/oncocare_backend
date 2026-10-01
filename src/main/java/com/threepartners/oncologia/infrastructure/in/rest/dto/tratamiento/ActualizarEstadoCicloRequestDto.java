package com.threepartners.oncologia.infrastructure.in.rest.dto.tratamiento;

import com.threepartners.oncologia.domain.tratamiento.EstadoCicloTratamiento;
import jakarta.validation.constraints.NotNull;

public record ActualizarEstadoCicloRequestDto(
        @NotNull(message = "El nuevo estado es obligatorio")
        EstadoCicloTratamiento estado,

        String observaciones
) {
}

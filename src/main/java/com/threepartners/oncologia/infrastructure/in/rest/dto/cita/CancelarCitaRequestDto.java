package com.threepartners.oncologia.infrastructure.in.rest.dto.cita;

import jakarta.validation.constraints.NotBlank;

public record CancelarCitaRequestDto(
        @NotBlank(message = "El motivo de cancelacion es obligatorio")
        String motivo
) {
}

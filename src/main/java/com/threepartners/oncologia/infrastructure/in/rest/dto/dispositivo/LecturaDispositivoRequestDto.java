package com.threepartners.oncologia.infrastructure.in.rest.dto.dispositivo;

import jakarta.validation.constraints.NotBlank;

public record LecturaDispositivoRequestDto(
        Long pacienteId,
        Long cicloTratamientoId,

        @NotBlank(message = "El tipo de dato es obligatorio")
        String tipoDato,

        @NotBlank(message = "El valor es obligatorio")
        String valor
) {
}

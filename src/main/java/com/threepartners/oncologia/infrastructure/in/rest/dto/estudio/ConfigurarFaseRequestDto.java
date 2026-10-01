package com.threepartners.oncologia.infrastructure.in.rest.dto.estudio;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record ConfigurarFaseRequestDto(

        @NotNull(message = "La fecha de inicio es obligatoria")
        LocalDate fechaInicio,

        @NotNull(message = "La fecha de fin es obligatoria")
        LocalDate fechaFin
) {
}

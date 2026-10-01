package com.threepartners.oncologia.infrastructure.in.rest.dto.cita;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

public record ReprogramarCitaRequestDto(
        @NotNull(message = "La nueva fecha es obligatoria")
        @FutureOrPresent(message = "La nueva fecha no puede ser en el pasado")
        LocalDate fecha,

        @NotNull(message = "La nueva hora es obligatoria")
        LocalTime hora
) {
}

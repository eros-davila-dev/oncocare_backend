package com.threepartners.oncologia.infrastructure.in.rest.dto.cita;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

public record CitaRequestDto(

        @NotNull(message = "El paciente es obligatorio")
        Long pacienteId,

        @NotNull(message = "El medico es obligatorio")
        Long medicoId,

        @NotNull(message = "La fecha es obligatoria")
        @FutureOrPresent(message = "La fecha de la cita no puede ser en el pasado")
        LocalDate fecha,

        @NotNull(message = "La hora es obligatoria")
        LocalTime hora,

        @NotBlank(message = "El tipo de consulta es obligatorio")
        String tipoConsulta,

        String observaciones
) {
}

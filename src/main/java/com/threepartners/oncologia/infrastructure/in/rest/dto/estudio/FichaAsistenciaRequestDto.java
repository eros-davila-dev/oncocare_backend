package com.threepartners.oncologia.infrastructure.in.rest.dto.estudio;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

public record FichaAsistenciaRequestDto(

        @NotNull(message = "El paciente es obligatorio")
        Long pacienteId,

        @NotNull(message = "La fecha de la cita es obligatoria")
        LocalDate fecha,

        LocalTime hora,

        @Size(max = 100, message = "El tipo de consulta no puede superar 100 caracteres")
        String tipoConsulta,

        @NotNull(message = "Indique si el paciente asistio")
        Boolean asistio
) {
}

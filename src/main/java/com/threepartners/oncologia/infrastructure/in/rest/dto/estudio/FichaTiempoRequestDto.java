package com.threepartners.oncologia.infrastructure.in.rest.dto.estudio;

import com.threepartners.oncologia.domain.estudio.TipoMedicion;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

public record FichaTiempoRequestDto(

        @NotNull(message = "El paciente es obligatorio")
        Long pacienteId,

        TipoMedicion tipo,

        @NotNull(message = "La fecha es obligatoria")
        LocalDate fecha,

        @NotNull(message = "La hora de inicio es obligatoria")
        LocalTime horaInicio,

        @NotNull(message = "La hora de fin es obligatoria")
        LocalTime horaFin,

        @Size(max = 500, message = "La observacion no puede superar 500 caracteres")
        String observacion
) {
}

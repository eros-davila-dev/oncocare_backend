package com.threepartners.oncologia.infrastructure.in.rest.dto.estudio;

import com.threepartners.oncologia.domain.estudio.CanalConsulta;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

public record FichaConsultaRequestDto(

        @NotNull(message = "La fecha es obligatoria")
        LocalDate fecha,

        LocalTime hora,

        @NotNull(message = "El medio de la consulta es obligatorio")
        CanalConsulta canal,

        Long pacienteId,

        @NotBlank(message = "Describa la consulta recibida")
        @Size(max = 300, message = "La descripcion no puede superar 300 caracteres")
        String resumen,

        @NotNull(message = "Indique si la consulta fue resuelta")
        Boolean resuelta,

        @Size(max = 500, message = "La observacion no puede superar 500 caracteres")
        String observacion
) {
}

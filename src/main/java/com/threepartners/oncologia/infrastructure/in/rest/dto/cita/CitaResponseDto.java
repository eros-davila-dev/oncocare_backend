package com.threepartners.oncologia.infrastructure.in.rest.dto.cita;

import com.threepartners.oncologia.domain.cita.EstadoCita;

import java.time.LocalDate;
import java.time.LocalTime;

public record CitaResponseDto(
        Long id,
        Long pacienteId,
        Long medicoId,
        LocalDate fecha,
        LocalTime hora,
        String tipoConsulta,
        EstadoCita estado,
        String observaciones
) {
}

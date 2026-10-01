package com.threepartners.oncologia.infrastructure.in.rest.dto.dispositivo;

import java.time.Instant;

public record LecturaDispositivoResponseDto(
        Long id,
        Long dispositivoId,
        Long pacienteId,
        Long cicloTratamientoId,
        String tipoDato,
        String valor,
        Instant fechaLectura
) {
}

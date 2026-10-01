package com.threepartners.oncologia.infrastructure.in.rest.dto.estudio;

import com.threepartners.oncologia.domain.estudio.EstadoFase;
import com.threepartners.oncologia.domain.estudio.Fase;
import com.threepartners.oncologia.domain.estudio.FaseEstudio;

import java.time.LocalDate;

public record FaseEstudioResponseDto(Fase fase, LocalDate fechaInicio, LocalDate fechaFin, EstadoFase estado) {

    public static FaseEstudioResponseDto de(FaseEstudio f) {
        return new FaseEstudioResponseDto(f.getFase(), f.getFechaInicio(), f.getFechaFin(), f.getEstado());
    }
}

package com.threepartners.oncologia.infrastructure.in.rest.dto.dashboard;

import com.threepartners.oncologia.infrastructure.in.rest.dto.estudio.IndicadoresDto;

import java.time.LocalDate;

/**
 * Dashboard de gestion: los indicadores de la tesis sobre todo el sistema
 * (alcance GLOBAL) en el periodo consultado.
 */
public record IndicadoresResponseDto(
        LocalDate desde,
        LocalDate hasta,
        IndicadoresDto indicadores,
        Double cumplimientoTratamientoPorcentaje
) {
}

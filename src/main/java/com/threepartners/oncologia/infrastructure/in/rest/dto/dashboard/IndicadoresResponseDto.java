package com.threepartners.oncologia.infrastructure.in.rest.dto.dashboard;

public record IndicadoresResponseDto(
        double tiempoPromedioRegistroSegundos,
        double tasaAusentismoPorcentaje,
        long consultasAtendidas,
        long citasTotales,
        double cumplimientoTratamientoPorcentaje
) {
}

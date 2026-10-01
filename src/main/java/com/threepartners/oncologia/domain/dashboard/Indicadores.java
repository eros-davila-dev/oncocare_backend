package com.threepartners.oncologia.domain.dashboard;

public record Indicadores(
        double tiempoPromedioRegistroSegundos,
        double tasaAusentismoPorcentaje,
        long consultasAtendidas,
        long citasTotales,
        double cumplimientoTratamientoPorcentaje
) {
}

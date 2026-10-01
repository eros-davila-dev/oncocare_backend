package com.threepartners.oncologia.domain.dashboard;

import com.threepartners.oncologia.domain.estudio.IndicadoresEstudio;
import com.threepartners.oncologia.domain.estudio.PeriodoMedicion;

/**
 * Vista de gestion diaria: los tres indicadores de la tesis sobre todo el
 * sistema (alcance GLOBAL) en un rango de fechas, mas el cumplimiento de
 * tratamiento como dato de contexto clinico.
 */
public record ResumenDashboard(
        PeriodoMedicion periodo,
        IndicadoresEstudio indicadores,
        Double cumplimientoTratamientoPorcentaje
) {
}

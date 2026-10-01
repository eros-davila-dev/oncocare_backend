package com.threepartners.oncologia.domain.estudio;

/**
 * Una fila de la tabla pareada (un participante, sus indicadores pretest y
 * postest): la entrada directa de la prueba de Wilcoxon para muestras
 * relacionadas.
 */
public record FilaPareada(
        String codigo,
        IndicadoresEstudio pretest,
        IndicadoresEstudio postest
) {
}

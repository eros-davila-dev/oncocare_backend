package com.threepartners.oncologia.domain.estudio;

/**
 * Salida de {@link PruebaWilcoxon}, con los mismos conceptos de la tabla
 * "Rangos" y "Estadisticos de prueba" de SPSS.
 *
 * @param pares            participantes con valor pretest y postest
 * @param empates          pares con diferencia cero (se descartan)
 * @param n                pares que entran a la prueba (pares - empates)
 * @param rangosNegativos  postest &lt; pretest (para TPR y TNS es la mejora esperada)
 * @param rangosPositivos  postest &gt; pretest (para NCA es la mejora esperada)
 * @param z                null si no hay pares
 * @param pExacta          null si no hay pares o n &gt; 50
 * @param tamanoEfecto     r = |Z| / raiz(pares) (Rosenthal)
 */
public record ResultadoWilcoxon(
        int pares,
        int empates,
        int n,
        int rangosNegativos,
        int rangosPositivos,
        Double sumaRangosNegativos,
        Double sumaRangosPositivos,
        Double medianaPretest,
        Double medianaPostest,
        Double z,
        Double pAsintotica,
        Double pExacta,
        Double tamanoEfecto
) {

    public static ResultadoWilcoxon sinDatos() {
        return new ResultadoWilcoxon(0, 0, 0, 0, 0, null, null, null, null, null, null, null, null);
    }
}

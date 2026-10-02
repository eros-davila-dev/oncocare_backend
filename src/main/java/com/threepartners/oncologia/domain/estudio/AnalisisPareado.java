package com.threepartners.oncologia.domain.estudio;

import java.util.List;
import java.util.function.Function;

/**
 * Wilcoxon de cada indicador sobre la tabla pareada. Cada indicador usa solo
 * los participantes con valor en ambas fases (un "Sin datos" no es un cero),
 * igual que SPSS al excluir casos con valores perdidos prueba por prueba.
 */
public record AnalisisPareado(
        ResultadoWilcoxon tiempoPromedioRegistro,
        ResultadoWilcoxon tasaAusentismo,
        ResultadoWilcoxon nivelConsultasAtendidas
) {

    public static AnalisisPareado de(List<FilaPareada> filas) {
        return new AnalisisPareado(
                prueba(filas, IndicadoresEstudio::tiempoPromedioRegistroMinutos),
                prueba(filas, IndicadoresEstudio::tasaAusentismo),
                prueba(filas, IndicadoresEstudio::nivelConsultasAtendidas));
    }

    private static ResultadoWilcoxon prueba(List<FilaPareada> filas, Function<IndicadoresEstudio, Double> indicador) {
        List<FilaPareada> completas = filas.stream()
                .filter(f -> indicador.apply(f.pretest()) != null && indicador.apply(f.postest()) != null)
                .toList();
        double[] pre = completas.stream().mapToDouble(f -> indicador.apply(f.pretest())).toArray();
        double[] post = completas.stream().mapToDouble(f -> indicador.apply(f.postest())).toArray();
        return PruebaWilcoxon.calcular(pre, post);
    }
}

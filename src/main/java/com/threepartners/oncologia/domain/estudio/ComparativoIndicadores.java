package com.threepartners.oncologia.domain.estudio;

/**
 * Pretest vs postest de los tres indicadores, con la diferencia absoluta y la
 * variacion relativa. Para TPR y TNS una variacion negativa es mejora; para
 * NCA, positiva.
 */
public record ComparativoIndicadores(
        AlcanceIndicador alcance,
        PeriodoMedicion periodoPretest,
        PeriodoMedicion periodoPostest,
        IndicadoresEstudio pretest,
        IndicadoresEstudio postest,
        Variacion tiempoPromedioRegistro,
        Variacion tasaAusentismo,
        Variacion nivelConsultasAtendidas
) {

    public static ComparativoIndicadores de(AlcanceIndicador alcance, PeriodoMedicion periodoPretest,
                                            PeriodoMedicion periodoPostest,
                                            IndicadoresEstudio pre, IndicadoresEstudio post) {
        return new ComparativoIndicadores(alcance, periodoPretest, periodoPostest, pre, post,
                Variacion.entre(pre.tiempoPromedioRegistroMinutos(), post.tiempoPromedioRegistroMinutos()),
                Variacion.entre(pre.tasaAusentismo(), post.tasaAusentismo()),
                Variacion.entre(pre.nivelConsultasAtendidas(), post.nivelConsultasAtendidas()));
    }

    public record Variacion(Double diferencia, Double porcentaje) {

        static Variacion entre(Double pretest, Double postest) {
            return new Variacion(
                    CalculoIndicadores.diferencia(pretest, postest),
                    CalculoIndicadores.variacionPorcentual(pretest, postest));
        }
    }
}

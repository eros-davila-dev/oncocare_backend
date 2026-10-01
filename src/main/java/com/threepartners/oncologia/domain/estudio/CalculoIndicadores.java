package com.threepartners.oncologia.domain.estudio;

/**
 * Formulas de los indicadores tal como estan en la matriz de
 * operacionalizacion (Anexo 1 de la tesis). Son funciones puras: la base de
 * datos solo aporta conteos, el calculo vive aqui y se prueba sin Spring.
 *
 * Cuando el denominador es cero devuelven null ("sin datos"), nunca 0: un 0 %
 * de ausentismo es un resultado; no haber tenido citas es otra cosa.
 */
public final class CalculoIndicadores {

    private static final double SEGUNDOS_POR_MINUTO = 60.0;
    private static final double CIEN = 100.0;

    private CalculoIndicadores() {
    }

    /**
     * TPR = suma(TRC) / NCR, en minutos.
     */
    public static Double tiempoPromedioRegistroMinutos(long sumaSegundos, long registros) {
        if (registros <= 0) {
            return null;
        }
        return sumaSegundos / (registros * SEGUNDOS_POR_MINUTO);
    }

    /**
     * TNS = NI / (NI + NCC) x 100. Las citas canceladas con aviso, las
     * futuras y las que aun no tienen desenlace NO entran al denominador.
     */
    public static Double tasaAusentismo(long inasistencias, long citasCumplidas) {
        long conDesenlace = inasistencias + citasCumplidas;
        if (conDesenlace <= 0) {
            return null;
        }
        return inasistencias * CIEN / conDesenlace;
    }

    /**
     * NCA = CA / TCR x 100, sobre consultas con resultado final.
     */
    public static Double nivelConsultasAtendidas(long consultasResueltas, long consultasCerradas) {
        if (consultasCerradas <= 0) {
            return null;
        }
        return consultasResueltas * CIEN / consultasCerradas;
    }

    /**
     * Variacion relativa (postest - pretest) / pretest x 100. Negativa en TPR
     * y TNS significa mejora; positiva en NCA significa mejora.
     */
    public static Double variacionPorcentual(Double pretest, Double postest) {
        if (pretest == null || postest == null || pretest == 0.0) {
            return null;
        }
        return (postest - pretest) * CIEN / pretest;
    }

    /**
     * Diferencia absoluta postest - pretest (en puntos porcentuales para TNS
     * y NCA, en minutos para TPR).
     */
    public static Double diferencia(Double pretest, Double postest) {
        if (pretest == null || postest == null) {
            return null;
        }
        return postest - pretest;
    }
}

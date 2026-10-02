package com.threepartners.oncologia.domain.estudio;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Valores de referencia calculados con scipy 1.15 (wilcoxon con
 * zero_method='wilcox', correction=False, method='approx', que reproduce la
 * salida asintotica de SPSS) y la p exacta por enumeracion de los 2^n signos.
 */
class PruebaWilcoxonTest {

    @Test
    void reproduceElEjemploClasicoConUnEmpateEnCeroYRangosRepetidos() {
        double[] pre = {110, 122, 125, 120, 140, 124, 123, 137, 135, 145};
        double[] post = {125, 115, 130, 140, 140, 115, 140, 125, 140, 135};

        ResultadoWilcoxon r = PruebaWilcoxon.calcular(pre, post);

        assertThat(r.pares()).isEqualTo(10);
        assertThat(r.empates()).isEqualTo(1);
        assertThat(r.n()).isEqualTo(9);
        assertThat(r.sumaRangosPositivos()).isEqualTo(27.0);
        assertThat(r.sumaRangosNegativos()).isEqualTo(18.0);
        assertThat(r.z()).isCloseTo(-0.5335822501, within(1e-9));
        assertThat(r.pAsintotica()).isCloseTo(0.5936305914, within(1e-9));
        assertThat(r.pExacta()).isCloseTo(0.6328125, within(1e-12));
    }

    @Test
    void tiempoDeRegistroQueBajaEnCasiTodosLosParticipantes() {
        double[] pre = {14.2, 12.5, 15.1, 11.8, 13.9, 16.3, 12.2, 14.8};
        double[] post = {9.1, 8.4, 10.2, 9.9, 7.5, 11.0, 13.0, 8.8};

        ResultadoWilcoxon r = PruebaWilcoxon.calcular(pre, post);

        assertThat(r.rangosNegativos()).isEqualTo(7);
        assertThat(r.rangosPositivos()).isEqualTo(1);
        assertThat(r.z()).isCloseTo(-2.3804761428, within(1e-9));
        assertThat(r.pAsintotica()).isCloseTo(0.0172902806, within(1e-9));
        assertThat(r.pExacta()).isCloseTo(0.015625, within(1e-12));
        assertThat(r.medianaPretest()).isCloseTo(14.05, within(1e-9));
        assertThat(r.tamanoEfecto()).isCloseTo(2.3804761428 / Math.sqrt(8), within(1e-9));
    }

    @Test
    void ausentismoPorParticipanteConMuchosEmpatesUsaLaCorreccionYLaExactaCondicional() {
        double[] pre = {100, 100, 50, 100, 0, 100, 50, 100, 100, 0, 100, 50};
        double[] post = {0, 0, 0, 50, 0, 100, 0, 0, 50, 0, 0, 0};

        ResultadoWilcoxon r = PruebaWilcoxon.calcular(pre, post);

        assertThat(r.empates()).isEqualTo(3);
        assertThat(r.n()).isEqualTo(9);
        assertThat(r.sumaRangosPositivos()).isZero();
        assertThat(r.z()).isCloseTo(-2.7386127875, within(1e-9));
        assertThat(r.pAsintotica()).isCloseTo(0.0061698993, within(1e-9));
        assertThat(r.pExacta()).isCloseTo(0.00390625, within(1e-12));
    }

    @Test
    void sinParesNoHayPruebaYSinDiferenciasNoHayEvidenciaDeCambio() {
        ResultadoWilcoxon vacio = PruebaWilcoxon.calcular(new double[0], new double[0]);
        assertThat(vacio.z()).isNull();
        assertThat(vacio.pAsintotica()).isNull();

        ResultadoWilcoxon iguales = PruebaWilcoxon.calcular(new double[]{5, 7}, new double[]{5, 7});
        assertThat(iguales.n()).isZero();
        assertThat(iguales.empates()).isEqualTo(2);
        assertThat(iguales.z()).isZero();
        assertThat(iguales.pAsintotica()).isEqualTo(1.0);
    }

    @Test
    void elAnalisisPareadoIgnoraAQuienLeFaltaUnaFaseEnEseIndicador() {
        var conAmbas = new IndicadoresEstudio(10.0, 1, 100.0, 1, 0, null, null, 0, 0, 0);
        var postConAmbas = new IndicadoresEstudio(6.0, 1, 0.0, 0, 1, null, null, 0, 0, 0);
        var sinPostest = new IndicadoresEstudio(null, 0, null, 0, 0, null, null, 0, 0, 0);

        AnalisisPareado analisis = AnalisisPareado.de(List.of(
                new FilaPareada("P01", conAmbas, postConAmbas),
                new FilaPareada("P02", conAmbas, sinPostest)));

        assertThat(analisis.tiempoPromedioRegistro().pares()).isEqualTo(1);
        assertThat(analisis.tasaAusentismo().pares()).isEqualTo(1);
        assertThat(analisis.nivelConsultasAtendidas().pares()).isZero();
    }
}

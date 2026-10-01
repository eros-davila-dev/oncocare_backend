package com.threepartners.oncologia.domain.estudio;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Formulas del Anexo 1 de la tesis. Cada caso borde documenta una decision de
 * la operacionalizacion (ver references/indicadores-tesis.md del skill).
 */
class CalculoIndicadoresTest {

    @Nested
    class TiempoPromedioRegistro {

        @Test
        void esLaSumaDeTiemposEntreElNumeroDeRegistrosEnMinutos() {
            // 3 registros: 600 s + 900 s + 300 s = 1800 s -> 1800 / 3 / 60 = 10 min
            assertThat(CalculoIndicadores.tiempoPromedioRegistroMinutos(1800, 3)).isEqualTo(10.0);
        }

        @Test
        void conservaLosDecimales() {
            assertThat(CalculoIndicadores.tiempoPromedioRegistroMinutos(150, 2)).isCloseTo(1.25, within(1e-9));
        }

        @Test
        void sinRegistrosEsSinDatosYNoCero() {
            assertThat(CalculoIndicadores.tiempoPromedioRegistroMinutos(0, 0)).isNull();
        }
    }

    @Nested
    class TasaAusentismo {

        @Test
        void esInasistenciasSobreCitasConDesenlace() {
            // 3 inasistencias, 6 cumplidas -> 3 / 9 = 33,33 %
            assertThat(CalculoIndicadores.tasaAusentismo(3, 6)).isCloseTo(33.333, within(0.001));
        }

        @Test
        void reproduceLaLineaBaseDeLaTesis() {
            // 65 % de ausentismo: 13 inasistencias de 20 citas con desenlace
            assertThat(CalculoIndicadores.tasaAusentismo(13, 7)).isEqualTo(65.0);
        }

        @Test
        void ceroInasistenciasEsUnResultadoValidoDeCeroPorCiento() {
            assertThat(CalculoIndicadores.tasaAusentismo(0, 5)).isEqualTo(0.0);
        }

        @Test
        void sinCitasConDesenlaceEsSinDatos() {
            assertThat(CalculoIndicadores.tasaAusentismo(0, 0)).isNull();
        }
    }

    @Nested
    class NivelConsultasAtendidas {

        @Test
        void esConsultasResueltasSobreConsultasCerradas() {
            assertThat(CalculoIndicadores.nivelConsultasAtendidas(45, 50)).isEqualTo(90.0);
        }

        @Test
        void sinConsultasCerradasEsSinDatos() {
            assertThat(CalculoIndicadores.nivelConsultasAtendidas(0, 0)).isNull();
        }
    }

    @Nested
    class Variaciones {

        @Test
        void unaReduccionDelTprEsUnaVariacionNegativa() {
            assertThat(CalculoIndicadores.variacionPorcentual(12.0, 3.0)).isEqualTo(-75.0);
            assertThat(CalculoIndicadores.diferencia(12.0, 3.0)).isEqualTo(-9.0);
        }

        @Test
        void sinLineaBaseNoHayVariacionRelativa() {
            assertThat(CalculoIndicadores.variacionPorcentual(0.0, 10.0)).isNull();
            assertThat(CalculoIndicadores.variacionPorcentual(null, 10.0)).isNull();
            assertThat(CalculoIndicadores.diferencia(5.0, null)).isNull();
        }
    }

    @Test
    void indicadoresSeConstruyenDesdeLosConteosConSuN() {
        var conteos = new ConteosIndicadores(4, 2400, 2, 8, 9, 6, 10);

        IndicadoresEstudio i = IndicadoresEstudio.de(conteos);

        assertThat(i.tiempoPromedioRegistroMinutos()).isEqualTo(10.0);
        assertThat(i.registros()).isEqualTo(4);
        assertThat(i.tasaAusentismo()).isEqualTo(20.0);
        assertThat(i.citasConDesenlace()).isEqualTo(10);
        assertThat(i.nivelConsultasAtendidas()).isEqualTo(90.0);
        assertThat(i.nivelConsultasAtendidasAutomatico()).isEqualTo(60.0);
    }
}

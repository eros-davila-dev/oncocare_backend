package com.threepartners.oncologia.domain.estudio;

import com.threepartners.oncologia.domain.shared.exception.ValidacionDeNegocioException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PeriodosYFasesTest {

    @Test
    void elPeriodoSeInterpretaEnHoraDeLimaConFinExclusivo() {
        var periodo = new PeriodoMedicion(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));

        // Lima es UTC-5: la medianoche local del 1/10 son las 05:00 UTC
        assertThat(periodo.inicio()).isEqualTo(Instant.parse("2026-10-01T05:00:00Z"));
        // el 31/10 a las 23:59 de Lima todavia pertenece al periodo
        assertThat(periodo.finExclusivo()).isEqualTo(Instant.parse("2026-11-01T05:00:00Z"));
    }

    @Test
    void elPeriodoNoAdmiteFinAnteriorAlInicio() {
        assertThatThrownBy(() -> new PeriodoMedicion(LocalDate.of(2026, 10, 2), LocalDate.of(2026, 10, 1)))
                .isInstanceOf(ValidacionDeNegocioException.class);
    }

    @Test
    void unaFaseCerradaYaNoPuedeCambiarSusFechas() {
        FaseEstudio fase = FaseEstudio.builder().fase(Fase.PRETEST).estado(EstadoFase.ABIERTA).build();
        fase.configurarFechas(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));
        fase.cerrar();

        assertThatThrownBy(() -> fase.configurarFechas(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 15)))
                .isInstanceOf(ValidacionDeNegocioException.class)
                .hasMessageContaining("cerrada");
    }

    @Test
    void laFaseContieneSusDiasLimiteInclusive() {
        FaseEstudio fase = FaseEstudio.builder().fase(Fase.POSTEST).estado(EstadoFase.ABIERTA)
                .fechaInicio(LocalDate.of(2026, 11, 1)).fechaFin(LocalDate.of(2026, 11, 30)).build();

        assertThat(fase.contiene(LocalDate.of(2026, 11, 1))).isTrue();
        assertThat(fase.contiene(LocalDate.of(2026, 11, 30))).isTrue();
        assertThat(fase.contiene(LocalDate.of(2026, 12, 1))).isFalse();
    }

    @Test
    void elFiltroUsaPorDefectoLaOperacionalizacionDeLaHipotesis() {
        var filtro = new FiltroIndicadores(new PeriodoMedicion(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 2)),
                null, null, null);

        assertThat(filtro.alcance()).isEqualTo(AlcanceIndicador.MUESTRA);
        assertThat(filtro.tipoRegistro()).isEqualTo(TipoMedicion.REGISTRO_CITA);
        assertThat(filtro.canalesRegistro()).containsExactlyInAnyOrder(CanalMedicion.INTRANET, CanalMedicion.MANUAL);
    }

    @Test
    void losCodigosDeParticipanteSonCorrelativos() {
        assertThat(ParticipanteEstudio.codigoParaNumero(1)).isEqualTo("P01");
        assertThat(ParticipanteEstudio.codigoParaNumero(30)).isEqualTo("P30");
        assertThat(ParticipanteEstudio.codigoParaNumero(100)).isEqualTo("P100");
    }
}

package com.threepartners.oncologia.domain.estudio;

import com.threepartners.oncologia.domain.shared.exception.ValidacionDeNegocioException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MedicionRegistroTest {

    private static final Instant INICIO = Instant.parse("2026-10-05T14:00:00Z");

    private MedicionRegistro abierta() {
        MedicionRegistro m = MedicionRegistro.iniciar(TipoMedicion.REGISTRO_CITA, CanalMedicion.INTRANET, 7L, INICIO);
        m.setId(1L);
        return m;
    }

    @Test
    void alCompletarseSellaElFinYCalculaLaDuracion() {
        MedicionRegistro m = abierta();

        m.completar(7L, TipoMedicion.REGISTRO_CITA, 3L, 90L, INICIO.plusSeconds(150));

        assertThat(m.getEstado()).isEqualTo(EstadoMedicion.COMPLETADA);
        assertThat(m.duracion()).isEqualTo(Duration.ofSeconds(150));
        assertThat(m.getPacienteId()).isEqualTo(3L);
        assertThat(m.getEntidadId()).isEqualTo(90L);
        assertThat(m.isSospechosa()).isFalse();
    }

    @Test
    void unRegistroDeMenosDeCincoSegundosQuedaMarcadoComoSospechosoPeroCuenta() {
        MedicionRegistro m = abierta();

        m.completar(7L, TipoMedicion.REGISTRO_CITA, 3L, 90L, INICIO.plusSeconds(2));

        assertThat(m.getEstado()).isEqualTo(EstadoMedicion.COMPLETADA);
        assertThat(m.isSospechosa()).isTrue();
    }

    @Test
    void noPuedeCerrarlaOtroUsuario() {
        MedicionRegistro m = abierta();

        assertThatThrownBy(() -> m.completar(8L, TipoMedicion.REGISTRO_CITA, 3L, 90L, INICIO.plusSeconds(60)))
                .isInstanceOf(ValidacionDeNegocioException.class)
                .hasMessageContaining("otro usuario");
    }

    @Test
    void noSePuedeReutilizarUnaSesionYaCerrada() {
        MedicionRegistro m = abierta();
        m.completar(7L, TipoMedicion.REGISTRO_CITA, 3L, 90L, INICIO.plusSeconds(60));

        assertThatThrownBy(() -> m.completar(7L, TipoMedicion.REGISTRO_CITA, 4L, 91L, INICIO.plusSeconds(120)))
                .isInstanceOf(ValidacionDeNegocioException.class);
    }

    @Test
    void noSePuedeCerrarConUnTipoDeRegistroDistinto() {
        MedicionRegistro m = abierta();

        assertThatThrownBy(() -> m.completar(7L, TipoMedicion.REGISTRO_PACIENTE, 3L, 3L, INICIO.plusSeconds(60)))
                .isInstanceOf(ValidacionDeNegocioException.class);
    }

    @Test
    void laFichaManualExigeQueElFinSeaPosteriorAlInicio() {
        assertThatThrownBy(() -> MedicionRegistro.manual(TipoMedicion.REGISTRO_CITA, 3L, INICIO, INICIO, 1L, null))
                .isInstanceOf(ValidacionDeNegocioException.class);
    }

    @Test
    void laFichaManualNaceCompletadaConCanalManual() {
        MedicionRegistro m = MedicionRegistro.manual(TipoMedicion.REGISTRO_CITA, 3L, INICIO, INICIO.plusSeconds(840), 1L, "obs");

        assertThat(m.getCanal()).isEqualTo(CanalMedicion.MANUAL);
        assertThat(m.getEstado()).isEqualTo(EstadoMedicion.COMPLETADA);
        assertThat(m.duracion().toMinutes()).isEqualTo(14);
    }

    @Test
    void soloSeAnulaUnaMedicionCompletada() {
        MedicionRegistro m = abierta();
        assertThatThrownBy(m::anular).isInstanceOf(ValidacionDeNegocioException.class);

        m.completar(7L, TipoMedicion.REGISTRO_CITA, 3L, 90L, INICIO.plusSeconds(60));
        m.anular();

        assertThat(m.getEstado()).isEqualTo(EstadoMedicion.ANULADA);
    }
}

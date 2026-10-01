package com.threepartners.oncologia.domain.cita;

import com.threepartners.oncologia.domain.shared.exception.ValidacionDeNegocioException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CitaTest {

    private static final Instant AHORA = Instant.parse("2026-10-05T15:00:00Z");

    private Cita programada() {
        return Cita.builder()
                .id(1L).pacienteId(3L).medicoId(2L)
                .fecha(LocalDate.of(2026, 10, 5)).hora(LocalTime.of(10, 0))
                .estado(EstadoCita.PROGRAMADA)
                .build();
    }

    @Test
    void registrarLaAtencionGuardaQuienYCuandoParaElIndicadorTns() {
        Cita cita = programada();

        cita.atender(9L, AHORA);

        assertThat(cita.getEstado()).isEqualTo(EstadoCita.ATENDIDA);
        assertThat(cita.getDesenlaceRegistradoPor()).isEqualTo(9L);
        assertThat(cita.getFechaHoraDesenlace()).isEqualTo(AHORA);
        assertThat(cita.isCierreAutomatico()).isFalse();
    }

    @Test
    void elCierreAutomaticoQuedaMarcado() {
        Cita cita = programada();

        cita.marcarNoAsistio(null, AHORA, true);

        assertThat(cita.getEstado()).isEqualTo(EstadoCita.NO_ASISTIO);
        assertThat(cita.isCierreAutomatico()).isTrue();
    }

    /**
     * Antes la reprogramacion devolvia cualquier cita a PROGRAMADA, incluso
     * una ya atendida o con inasistencia, borrando su desenlace y alterando
     * el TNS. Ahora es una regla de dominio.
     */
    @ParameterizedTest
    @EnumSource(value = EstadoCita.class, names = {"ATENDIDA", "NO_ASISTIO", "CANCELADA"})
    void unaCitaEnEstadoFinalNoPuedeReprogramarse(EstadoCita estadoFinal) {
        Cita cita = programada();
        cita.setEstado(estadoFinal);

        assertThatThrownBy(() -> cita.reprogramar(LocalDate.of(2026, 10, 9), LocalTime.of(11, 0)))
                .isInstanceOf(ValidacionDeNegocioException.class);
        assertThat(cita.getEstado()).isEqualTo(estadoFinal);
    }

    @Test
    void reprogramarMueveLaMismaCitaYCuentaLasReprogramaciones() {
        Cita cita = programada();
        cita.confirmar();

        cita.reprogramar(LocalDate.of(2026, 10, 9), LocalTime.of(11, 0));
        cita.reprogramar(LocalDate.of(2026, 10, 12), LocalTime.of(9, 0));

        assertThat(cita.getEstado()).isEqualTo(EstadoCita.PROGRAMADA);
        assertThat(cita.getFecha()).isEqualTo(LocalDate.of(2026, 10, 12));
        assertThat(cita.getVecesReprogramada()).isEqualTo(2);
    }

    @Test
    void laCapturaDelPretestNaceConSuDesenlaceYSinMedico() {
        Cita cita = Cita.capturaPretest(3L, LocalDate.of(2026, 9, 10), null, "Control", false, 4L, AHORA);

        assertThat(cita.getEstado()).isEqualTo(EstadoCita.NO_ASISTIO);
        assertThat(cita.getOrigen()).isEqualTo(OrigenCita.CAPTURA_PRETEST);
        assertThat(cita.getMedicoId()).isNull();
        assertThat(cita.getHora()).isEqualTo(LocalTime.MIDNIGHT);
        assertThat(cita.getDesenlaceRegistradoPor()).isEqualTo(4L);
    }
}

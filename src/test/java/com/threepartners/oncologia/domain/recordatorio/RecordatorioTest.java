package com.threepartners.oncologia.domain.recordatorio;

import com.threepartners.oncologia.domain.cita.Cita;
import com.threepartners.oncologia.domain.cita.EstadoCita;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class RecordatorioTest {

    /** 5/10/2026 10:00 en Lima. */
    private static final Instant AHORA = Instant.parse("2026-10-05T15:00:00Z");

    private Cita cita(LocalDate fecha, LocalTime hora) {
        return Cita.builder().id(1L).pacienteId(2L).fecha(fecha).hora(hora).estado(EstadoCita.PROGRAMADA).build();
    }

    @Test
    void porTelegramSeProgramanLosTresAvisosSiLaCitaEsLejana() {
        var plan = Recordatorio.planificar(cita(LocalDate.of(2026, 10, 9), LocalTime.of(9, 0)), CanalRecordatorio.TELEGRAM, AHORA);

        assertThat(plan).extracting(Recordatorio::getTipo)
                .containsExactly(TipoRecordatorio.T72H, TipoRecordatorio.T24H, TipoRecordatorio.T2H);
        // 72 h antes del 9/10 09:00 Lima = 6/10 09:00 Lima = 14:00 UTC
        assertThat(plan.getFirst().getProgramadoPara()).isEqualTo(Instant.parse("2026-10-06T14:00:00Z"));
    }

    @Test
    void soloSeProgramanLosMomentosQueAunNoPasaron() {
        // Cita manana a las 11:00: el aviso de 72 h ya paso
        var plan = Recordatorio.planificar(cita(LocalDate.of(2026, 10, 6), LocalTime.of(11, 0)), CanalRecordatorio.TELEGRAM, AHORA);

        assertThat(plan).extracting(Recordatorio::getTipo).containsExactly(TipoRecordatorio.T24H, TipoRecordatorio.T2H);
    }

    @Test
    void porLlamadaBastaUnAvisoElDiaAnterior() {
        var plan = Recordatorio.planificar(cita(LocalDate.of(2026, 10, 9), LocalTime.of(9, 0)), CanalRecordatorio.LLAMADA, AHORA);

        assertThat(plan).extracting(Recordatorio::getTipo).containsExactly(TipoRecordatorio.T24H);
    }

    @Test
    void dejaDeEstarVigenteSiLaCitaSeReprogramaOSeCancela() {
        Cita cita = cita(LocalDate.of(2026, 10, 9), LocalTime.of(9, 0));
        Recordatorio aviso = Recordatorio.planificar(cita, CanalRecordatorio.TELEGRAM, AHORA).get(1);
        assertThat(aviso.vigentePara(cita, AHORA)).isTrue();

        cita.reprogramar(LocalDate.of(2026, 10, 12), LocalTime.of(9, 0));
        assertThat(aviso.vigentePara(cita, AHORA)).isFalse();

        Cita cancelada = cita(LocalDate.of(2026, 10, 9), LocalTime.of(9, 0));
        cancelada.cancelar();
        assertThat(aviso.vigentePara(cancelada, AHORA)).isFalse();
    }

    @Test
    void unFalloTransitorioVuelveALaColaHastaAgotarLosIntentos() {
        Recordatorio aviso = Recordatorio.builder().id(9L).estado(EstadoRecordatorio.PENDIENTE).build();

        for (int i = 1; i < Recordatorio.MAXIMO_INTENTOS; i++) {
            aviso.tomar(AHORA);
            aviso.marcarFallido("timeout");
            assertThat(aviso.getEstado()).isEqualTo(EstadoRecordatorio.PENDIENTE);
        }
        aviso.tomar(AHORA);
        aviso.marcarFallido("timeout");

        assertThat(aviso.getEstado()).isEqualTo(EstadoRecordatorio.FALLIDO);
        assertThat(aviso.getIntentos()).isEqualTo(Recordatorio.MAXIMO_INTENTOS);
    }

    @Test
    void unAvisoTomadoPeroNuncaConfirmadoSeLiberaParaReintentar() {
        Recordatorio aviso = Recordatorio.builder().id(9L).estado(EstadoRecordatorio.PENDIENTE).build();
        aviso.tomar(AHORA);

        aviso.liberar();

        assertThat(aviso.getEstado()).isEqualTo(EstadoRecordatorio.PENDIENTE);
    }
}

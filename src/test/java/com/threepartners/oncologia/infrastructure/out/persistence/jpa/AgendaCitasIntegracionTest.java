package com.threepartners.oncologia.infrastructure.out.persistence.jpa;

import com.threepartners.oncologia.domain.cita.CitaAgenda;
import com.threepartners.oncologia.soporte.PostgresIntegracionTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Consultas de la agenda de recepcion y del cierre automatico (indicador TNS).
 */
class AgendaCitasIntegracionTest extends PostgresIntegracionTest {

    @Autowired
    private CitaRepositoryAdapter citas;
    @Autowired
    private JdbcTemplate jdbc;

    private long medico;

    @BeforeEach
    void sembrar() {
        jdbc.execute("TRUNCATE notificacion, conversacion_chatbot, lectura_dispositivo, documento_paciente, "
                + "ciclo_tratamiento, cita, participante_estudio, medicion_registro, consulta, paciente RESTART IDENTITY CASCADE");
        medico = jdbc.queryForObject("SELECT MIN(id) FROM usuario", Long.class);
        long paciente = jdbc.queryForObject("INSERT INTO paciente (nombres, apellidos, documento_identidad, fecha_nacimiento, telefono) "
                + "VALUES ('Rosa', 'Quispe', '40000001', '1960-01-01', '987654321') RETURNING id", Long.class);
        cita(paciente, medico, "2026-10-05", "11:00", "PROGRAMADA", "INTRANET");
        cita(paciente, medico, "2026-10-05", "08:30", "CONFIRMADA", "INTRANET");
        cita(paciente, null, "2026-10-05", "09:00", "ATENDIDA", "CAPTURA_PRETEST");   // no es agenda
        cita(paciente, medico, "2026-10-03", "10:00", "CONFIRMADA", "PORTAL");         // pendiente de cierre
        cita(paciente, medico, "2026-10-02", "10:00", "ATENDIDA", "INTRANET");         // ya cerrada
    }

    @Test
    void laAgendaDelDiaVieneOrdenadaPorHoraConLosDatosDelPacienteYSinCapturasDelPretest() {
        var agenda = citas.agendaDelDia(LocalDate.of(2026, 10, 5), null);

        assertThat(agenda).extracting(c -> c.cita().getHora().toString()).containsExactly("08:30", "11:00");
        CitaAgenda primera = agenda.getFirst();
        assertThat(primera.pacienteNombre()).isEqualTo("Rosa Quispe");
        assertThat(primera.pacienteTelefono()).isEqualTo("987654321");
        assertThat(primera.medicoNombre()).isNotBlank();
        assertThat(citas.agendaDelDia(LocalDate.of(2026, 10, 5), medico + 999)).isEmpty();
    }

    @Test
    void pendientesDeCierreSonCitasPasadasSinDesenlace() {
        var pendientes = citas.pendientesDeCierre(LocalDate.of(2026, 10, 5));

        assertThat(pendientes).hasSize(1);
        assertThat(pendientes.getFirst().cita().getFecha()).isEqualTo(LocalDate.of(2026, 10, 3));
    }

    @Test
    void elCierreAutomaticoSoloTomaCitasAnterioresAlLimite() {
        assertThat(citas.sinDesenlaceAntesDe(LocalDateTime.of(2026, 10, 3, 9, 59))).isEmpty();
        assertThat(citas.sinDesenlaceAntesDe(LocalDateTime.of(2026, 10, 3, 10, 1))).hasSize(1);
    }

    private void cita(long paciente, Long medicoId, String fecha, String hora, String estado, String origen) {
        jdbc.update("INSERT INTO cita (paciente_id, medico_id, fecha, hora, estado, origen) VALUES (?, ?, ?::date, ?::time, ?, ?)",
                paciente, medicoId, fecha, hora, estado, origen);
    }
}

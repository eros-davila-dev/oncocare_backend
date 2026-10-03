package com.threepartners.oncologia.infrastructure.out.persistence.jdbc;

import com.threepartners.oncologia.domain.estudio.CategoriaConsulta;
import com.threepartners.oncologia.domain.estudio.PeriodoMedicion;
import com.threepartners.oncologia.domain.estudio.RecoleccionSesiones;
import com.threepartners.oncologia.soporte.PostgresIntegracionTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contra PostgreSQL real (incluye las migraciones V11 y V12): que filas entran
 * a cada ficha de la recoleccion por sesion.
 */
class RecoleccionEstudioJdbcAdapterIntegracionTest extends PostgresIntegracionTest {

    private static final PeriodoMedicion OCTUBRE = new PeriodoMedicion(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));

    @Autowired
    private RecoleccionEstudioJdbcAdapter adapter;
    @Autowired
    private JdbcTemplate jdbc;

    private long paciente;
    private long medico;

    @BeforeEach
    void sembrar() {
        jdbc.execute("TRUNCATE medicion_registro, consulta, correccion_medicion, participante_estudio, estudio_fase, "
                + "recordatorio, notificacion, conversacion_chatbot, lectura_dispositivo, documento_paciente, "
                + "ciclo_tratamiento, cita, paciente RESTART IDENTITY CASCADE");
        medico = jdbc.queryForObject("SELECT MIN(id) FROM usuario", Long.class);
        paciente = jdbc.queryForObject("INSERT INTO paciente (nombres, apellidos, documento_identidad, fecha_nacimiento) "
                + "VALUES ('Ana', 'Perez', '10000001', '1970-01-01') RETURNING id", Long.class);
    }

    @Test
    void elTprSoloTomaAltasDePacienteHechasPorElPersonal() {
        medicion("REGISTRO_PACIENTE", "INTRANET", "COMPLETADA", "2026-10-05T14:00:00Z", 180);
        medicion("REGISTRO_PACIENTE", "PORTAL", "COMPLETADA", "2026-10-05T14:10:00Z", 600);
        medicion("REGISTRO_CITA", "INTRANET", "COMPLETADA", "2026-10-05T14:20:00Z", 60);
        medicion("REGISTRO_PACIENTE", "INTRANET", "ABANDONADA", "2026-10-05T14:30:00Z", null);

        var tiempos = adapter.filas(OCTUBRE).tiempos();

        assertThat(tiempos).hasSize(1);
        assertThat(tiempos.getFirst().minutos()).isEqualTo(3.0);
        assertThat(tiempos.getFirst().codigo()).isEqualTo("PAC-0001");
        assertThat(tiempos.getFirst().fecha()).isEqualTo(LocalDate.of(2026, 10, 5)); // 09:00 en Lima
    }

    @Test
    void lasCitasTraenSiSeEnvioElRecordatorio() {
        long conAviso = cita("2026-10-05", "ATENDIDA");
        cita("2026-10-05", "NO_ASISTIO");
        jdbc.update("INSERT INTO recordatorio (cita_id, tipo, canal, programado_para, estado) "
                + "VALUES (?, 'T24H', 'TELEGRAM', now(), 'ENVIADO')", conAviso);

        var citas = adapter.filas(OCTUBRE).citas();

        assertThat(citas).extracting(RecoleccionSesiones.FilaCita::recordatorioEnviado).containsExactly(true, false);
        assertThat(citas).extracting(RecoleccionSesiones.FilaCita::estado).containsExactly("ATENDIDA", "NO_ASISTIO");
    }

    @Test
    void lasConsultasAnuladasNoEntranYSeLeenCategoriaDerivacionYReapertura() {
        consulta("RESUELTA_BOT", "CITAS", false, false);
        consulta("ESCALADA", "HORARIOS", true, false);
        consulta("ANULADA", "CITAS", false, false);

        var consultas = adapter.filas(OCTUBRE).consultas();

        assertThat(consultas).hasSize(2);
        assertThat(consultas.get(0).categoria()).isEqualTo(CategoriaConsulta.CITAS);
        assertThat(consultas.get(0).resueltaEnPrimerContacto()).isTrue();
        assertThat(consultas.get(1).derivadaAlPersonal()).isTrue();
        assertThat(consultas.get(0).tiempoRespuestaMin()).isEqualTo(0.5);
    }

    private void medicion(String tipo, String canal, String estado, String inicioIso, Integer segundos) {
        Instant inicio = Instant.parse(inicioIso);
        Timestamp fin = segundos != null ? Timestamp.from(inicio.plusSeconds(segundos)) : null;
        jdbc.update("INSERT INTO medicion_registro (tipo, canal, estado, paciente_id, inicio, fin) VALUES (?, ?, ?, ?, ?, ?)",
                tipo, canal, estado, paciente, Timestamp.from(inicio), fin);
    }

    private long cita(String fecha, String estado) {
        return jdbc.queryForObject("INSERT INTO cita (paciente_id, medico_id, fecha, hora, estado) "
                + "VALUES (?, ?, ?::date, '10:00', ?) RETURNING id", Long.class, paciente, medico, fecha, estado);
    }

    private void consulta(String resultado, String categoria, boolean derivada, boolean reabierta) {
        jdbc.update("INSERT INTO consulta (canal, paciente_id, resultado, abierta_en, categoria, derivada, reabierta, "
                        + "tiempo_primera_respuesta_ms) VALUES ('CHATBOT_WEB', ?, ?, '2026-10-05T16:00:00Z', ?, ?, ?, 30000)",
                paciente, resultado, categoria, derivada, reabierta);
    }
}

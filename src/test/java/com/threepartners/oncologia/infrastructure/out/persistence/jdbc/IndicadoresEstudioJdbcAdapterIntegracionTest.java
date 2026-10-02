package com.threepartners.oncologia.infrastructure.out.persistence.jdbc;

import com.threepartners.oncologia.domain.estudio.AlcanceIndicador;
import com.threepartners.oncologia.domain.estudio.CanalMedicion;
import com.threepartners.oncologia.domain.estudio.ConteosIndicadores;
import com.threepartners.oncologia.domain.estudio.FiltroIndicadores;
import com.threepartners.oncologia.domain.estudio.IndicadoresEstudio;
import com.threepartners.oncologia.domain.estudio.PeriodoMedicion;
import com.threepartners.oncologia.domain.estudio.RegistrosFichas;
import com.threepartners.oncologia.domain.estudio.ResultadoConsulta;
import com.threepartners.oncologia.domain.estudio.TipoMedicion;
import com.threepartners.oncologia.soporte.PostgresIntegracionTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * Escenario conocido que fija, contra PostgreSQL real, cada decision de la
 * operacionalizacion de los indicadores: que cuenta, que no, y en que fase.
 *
 * Pacientes: A (participante P01 incluido), B (P02 excluido), C (no participante).
 * Periodo: septiembre de 2026 (hora de Lima).
 */
class IndicadoresEstudioJdbcAdapterIntegracionTest extends PostgresIntegracionTest {

    private static final PeriodoMedicion SEPTIEMBRE = new PeriodoMedicion(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));

    @Autowired
    private IndicadoresEstudioJdbcAdapter adapter;
    @Autowired
    private JdbcTemplate jdbc;

    private long pacienteA;
    private long pacienteB;
    private long pacienteC;
    private long medico;

    @BeforeEach
    void sembrarEscenario() {
        jdbc.execute("TRUNCATE medicion_registro, consulta, correccion_medicion, participante_estudio, estudio_fase, "
                + "notificacion, conversacion_chatbot, lectura_dispositivo, documento_paciente, ciclo_tratamiento, cita, paciente "
                + "RESTART IDENTITY CASCADE");
        medico = jdbc.queryForObject("SELECT MIN(id) FROM usuario", Long.class);
        pacienteA = paciente("10000001");
        pacienteB = paciente("10000002");
        pacienteC = paciente("10000003");
        jdbc.update("INSERT INTO participante_estudio (paciente_id, codigo, fecha_consentimiento, incluido) VALUES (?, 'P01', '2026-08-20', TRUE)", pacienteA);
        jdbc.update("INSERT INTO participante_estudio (paciente_id, codigo, fecha_consentimiento, incluido, motivo_exclusion) "
                + "VALUES (?, 'P02', '2026-08-20', FALSE, 'RETIRO_CONSENTIMIENTO')", pacienteB);

        // TPR (tipo REGISTRO_CITA)
        medicion(pacienteA, "INTRANET", "COMPLETADA", "2026-09-10T15:00:00Z", 600);
        medicion(pacienteA, "MANUAL", "COMPLETADA", "2026-09-11T15:00:00Z", 300);
        medicion(pacienteA, "INTRANET", "COMPLETADA", "2026-10-01T04:30:00Z", 180);   // 30/09 23:30 en Lima: dentro
        medicion(pacienteA, "INTRANET", "ABANDONADA", "2026-09-12T15:00:00Z", null);   // no cuenta
        medicion(pacienteA, "INTRANET", "ANULADA", "2026-09-13T15:00:00Z", 1000);      // no cuenta
        medicion(pacienteA, "PORTAL", "COMPLETADA", "2026-09-14T15:00:00Z", 120);      // canal fuera de la hipotesis
        medicion(pacienteA, "INTRANET", "COMPLETADA", "2026-10-02T15:00:00Z", 900);    // fuera del periodo
        medicion(pacienteB, "INTRANET", "COMPLETADA", "2026-09-15T15:00:00Z", 60);     // excluido: solo GLOBAL
        medicion(pacienteC, "INTRANET", "COMPLETADA", "2026-09-16T15:00:00Z", 1200);   // no participante: solo GLOBAL

        // TNS
        cita(pacienteA, "2026-09-03", "ATENDIDA");
        cita(pacienteA, "2026-09-10", "ATENDIDA");
        cita(pacienteA, "2026-09-17", "NO_ASISTIO");
        cita(pacienteA, "2026-09-20", "CANCELADA");      // con aviso: fuera del denominador
        cita(pacienteA, "2026-09-28", "PROGRAMADA");     // sin desenlace: fuera
        cita(pacienteA, "2026-10-01", "NO_ASISTIO");     // fuera del periodo
        cita(pacienteC, "2026-09-05", "NO_ASISTIO");     // solo GLOBAL

        // NCA
        consulta(pacienteA, "RESUELTA_BOT", "2026-09-04T15:00:00Z");
        consulta(pacienteA, "RESUELTA_PERSONAL", "2026-09-05T15:00:00Z");
        consulta(pacienteA, "NO_RESUELTA", "2026-09-06T15:00:00Z");
        consulta(pacienteA, "ESCALADA", "2026-09-07T15:00:00Z");     // esperando al personal: fuera
        consulta(pacienteA, "ANULADA", "2026-09-08T15:00:00Z");      // fuera
        consulta(pacienteA, null, "2026-09-09T15:00:00Z");           // abierta: fuera
        consulta(null, "RESUELTA_BOT", "2026-09-10T15:00:00Z");      // visitante anonimo: solo GLOBAL
    }

    @Test
    void muestraCuentaSoloParticipantesIncluidosYAplicaCadaReglaDeExclusion() {
        IndicadoresEstudio i = IndicadoresEstudio.de(adapter.contar(FiltroIndicadores.deHipotesis(SEPTIEMBRE, AlcanceIndicador.MUESTRA)));

        // TPR: (600 + 300 + 180) / 3 = 360 s = 6 min
        assertThat(i.registros()).isEqualTo(3);
        assertThat(i.tiempoPromedioRegistroMinutos()).isEqualTo(6.0);
        // TNS: 1 / (1 + 2)
        assertThat(i.inasistencias()).isEqualTo(1);
        assertThat(i.citasCumplidas()).isEqualTo(2);
        assertThat(i.tasaAusentismo()).isCloseTo(33.333, within(0.001));
        // NCA: 2 resueltas de 3 cerradas; 1 por el bot
        assertThat(i.consultasCerradas()).isEqualTo(3);
        assertThat(i.nivelConsultasAtendidas()).isCloseTo(66.667, within(0.001));
        assertThat(i.nivelConsultasAtendidasAutomatico()).isCloseTo(33.333, within(0.001));
    }

    @Test
    void globalIncluyeATodosLosPacientesYVisitantes() {
        IndicadoresEstudio i = IndicadoresEstudio.de(adapter.contar(FiltroIndicadores.deHipotesis(SEPTIEMBRE, AlcanceIndicador.GLOBAL)));

        // TPR: (600 + 300 + 180 + 60 + 1200) / 5 = 468 s = 7.8 min
        assertThat(i.registros()).isEqualTo(5);
        assertThat(i.tiempoPromedioRegistroMinutos()).isCloseTo(7.8, within(1e-9));
        // TNS: 2 / (2 + 2)
        assertThat(i.tasaAusentismo()).isEqualTo(50.0);
        // NCA: 3 / 4
        assertThat(i.nivelConsultasAtendidas()).isEqualTo(75.0);
    }

    @Test
    void elFiltroDeCanalesPermiteReportarElAutoservicioAparte() {
        var soloPortal = new FiltroIndicadores(SEPTIEMBRE, AlcanceIndicador.MUESTRA, TipoMedicion.REGISTRO_CITA,
                EnumSet.of(CanalMedicion.PORTAL));

        ConteosIndicadores c = adapter.contar(soloPortal);

        assertThat(c.registros()).isEqualTo(1);
        assertThat(c.sumaSegundosRegistro()).isEqualTo(120);
    }

    @Test
    void porParticipanteDevuelveSoloLaMuestraIncluida() {
        var porParticipante = adapter.contarPorParticipante(FiltroIndicadores.deHipotesis(SEPTIEMBRE, AlcanceIndicador.MUESTRA));

        assertThat(porParticipante).containsOnlyKeys("P01");
        ConteosIndicadores p01 = porParticipante.get("P01");
        assertThat(p01.registros()).isEqualTo(3);
        assertThat(p01.inasistencias()).isEqualTo(1);
        assertThat(p01.consultasResueltas()).isEqualTo(2);
    }

    @Test
    void lasFilasExportadasSumanExactamenteLoQueMuestraElIndicador() {
        for (AlcanceIndicador alcance : AlcanceIndicador.values()) {
            var filtro = FiltroIndicadores.deHipotesis(SEPTIEMBRE, alcance);
            ConteosIndicadores conteos = adapter.contar(filtro);

            var tiempos = adapter.tiempos(filtro);
            assertThat(tiempos).hasSize((int) conteos.registros());
            assertThat(tiempos.stream().mapToLong(RegistrosFichas.Tiempo::segundos).sum())
                    .isEqualTo(conteos.sumaSegundosRegistro());

            var asistencias = adapter.asistencias(filtro);
            assertThat(asistencias.stream().filter(a -> !a.asistio()).count()).isEqualTo(conteos.inasistencias());
            assertThat(asistencias.stream().filter(RegistrosFichas.Asistencia::asistio).count())
                    .isEqualTo(conteos.citasCumplidas());

            var consultas = adapter.consultas(filtro);
            assertThat(consultas).hasSize((int) conteos.consultasCerradas());
            assertThat(consultas.stream().filter(k -> k.resultado() != ResultadoConsulta.NO_RESUELTA).count())
                    .isEqualTo(conteos.consultasResueltas());
        }
    }

    @Test
    void lasFilasLlevanSoloElCodigoYLaHoraDeLima() {
        var muestra = FiltroIndicadores.deHipotesis(SEPTIEMBRE, AlcanceIndicador.MUESTRA);
        var global = FiltroIndicadores.deHipotesis(SEPTIEMBRE, AlcanceIndicador.GLOBAL);

        assertThat(adapter.tiempos(muestra)).extracting(RegistrosFichas.Tiempo::codigo).containsOnly("P01");
        // 2026-10-01T04:30Z es 30/09 23:30 en Lima
        assertThat(adapter.tiempos(muestra).getLast().fecha()).isEqualTo(LocalDate.of(2026, 9, 30));
        assertThat(adapter.tiempos(muestra).getLast().horaInicio()).isEqualTo(LocalTime.of(23, 30));
        // Fuera de la muestra (P02 excluido, C no participante, visitante) el codigo va vacio
        assertThat(adapter.tiempos(global)).extracting(RegistrosFichas.Tiempo::codigo)
                .containsExactlyInAnyOrder("P01", "P01", "P01", null, null);
        assertThat(adapter.consultas(global)).extracting(RegistrosFichas.ConsultaCerrada::codigo).contains((String) null);
    }

    @Test
    void unPeriodoSinDatosDevuelveConteosEnCeroEIndicadoresSinDatos() {
        var vacio = new PeriodoMedicion(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31));

        IndicadoresEstudio i = IndicadoresEstudio.de(adapter.contar(FiltroIndicadores.deHipotesis(vacio, AlcanceIndicador.GLOBAL)));

        assertThat(i.tiempoPromedioRegistroMinutos()).isNull();
        assertThat(i.tasaAusentismo()).isNull();
        assertThat(i.nivelConsultasAtendidas()).isNull();
    }

    @Test
    void laBaseDeDatosImpideBorrarOReescribirMedicionesCerradas() {
        Long id = jdbc.queryForObject("SELECT MIN(id) FROM medicion_registro WHERE estado = 'COMPLETADA'", Long.class);

        assertThatThrownBy(() -> jdbc.update("DELETE FROM medicion_registro WHERE id = ?", id))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("solo insercion");
        assertThatThrownBy(() -> jdbc.update("UPDATE medicion_registro SET fin = fin + interval '1 hour' WHERE id = ?", id))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM consulta"))
                .isInstanceOf(DataAccessException.class);
    }

    private long paciente(String documento) {
        return jdbc.queryForObject("INSERT INTO paciente (nombres, apellidos, documento_identidad, fecha_nacimiento) "
                + "VALUES ('Paciente', ?, ?, '1970-01-01') RETURNING id", Long.class, documento, documento);
    }

    private void medicion(long pacienteId, String canal, String estado, String inicioIso, Integer segundos) {
        Instant inicio = Instant.parse(inicioIso);
        Timestamp fin = segundos != null ? Timestamp.from(inicio.plusSeconds(segundos)) : null;
        jdbc.update("INSERT INTO medicion_registro (tipo, canal, estado, paciente_id, inicio, fin) VALUES ('REGISTRO_CITA', ?, ?, ?, ?, ?)",
                canal, estado, pacienteId, Timestamp.from(inicio), fin);
    }

    private void cita(long pacienteId, String fecha, String estado) {
        jdbc.update("INSERT INTO cita (paciente_id, medico_id, fecha, hora, estado) VALUES (?, ?, ?::date, '10:00', ?)",
                pacienteId, medico, fecha, estado);
    }

    private void consulta(Long pacienteId, String resultado, String abiertaIso) {
        jdbc.update("INSERT INTO consulta (canal, paciente_id, resultado, abierta_en) VALUES ('CHATBOT_WEB', ?, ?, ?)",
                pacienteId, resultado, Timestamp.from(Instant.parse(abiertaIso)));
    }
}

package com.threepartners.oncologia.application.recordatorio;

import com.threepartners.oncologia.application.chatbot.GestorConsultasChatbot;
import com.threepartners.oncologia.application.cita.AccionesCitaPacienteService;
import com.threepartners.oncologia.application.recordatorio.EntregarRecordatoriosUseCase.RecordatorioParaEnviar;
import com.threepartners.oncologia.application.recordatorio.ResponderRecordatorioUseCase.AccionRecordatorio;
import com.threepartners.oncologia.application.telegram.VinculacionTelegramUseCase;
import com.threepartners.oncologia.config.FrontendProperties;
import com.threepartners.oncologia.config.TelegramProperties;
import com.threepartners.oncologia.domain.cita.CitaRepositoryPort;
import com.threepartners.oncologia.domain.cita.EstadoCita;
import com.threepartners.oncologia.domain.estudio.ConsultaRepositoryPort;
import com.threepartners.oncologia.domain.paciente.PacienteRepositoryPort;
import com.threepartners.oncologia.domain.paciente.TokenVinculacionTelegramRepositoryPort;
import com.threepartners.oncologia.domain.recordatorio.EstadoRecordatorio;
import com.threepartners.oncologia.domain.recordatorio.RecordatorioRepositoryPort;
import com.threepartners.oncologia.domain.shared.ZonaHoraria;
import com.threepartners.oncologia.domain.usuario.GeneradorTokenPort;
import com.threepartners.oncologia.soporte.PostgresIntegracionTest;
import com.threepartners.oncologia.soporte.RelojDePrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Ciclo completo de los recordatorios (indicador TNS) contra PostgreSQL y con
 * un reloj controlado: vinculacion de Telegram, programacion idempotente,
 * entrega a n8n, respuesta con boton, cancelacion por reprogramacion y
 * llamadas de recepcion para quien no tiene Telegram.
 */
class FlujoRecordatoriosIntegracionTest extends PostgresIntegracionTest {

    /** 5/10/2026 10:00 en Lima. */
    private static final Instant INICIO = Instant.parse("2026-10-05T15:00:00Z");
    private static final long CHAT_ANA = 555_111L;

    @Autowired private JdbcTemplate jdbc;
    @Autowired private TransactionTemplate tx;
    @Autowired private CitaRepositoryPort citas;
    @Autowired private PacienteRepositoryPort pacientes;
    @Autowired private RecordatorioRepositoryPort recordatorios;
    @Autowired private TokenVinculacionTelegramRepositoryPort tokens;
    @Autowired private ConsultaRepositoryPort consultas;
    @Autowired private GeneradorTokenPort generadorToken;
    @Autowired private ApplicationEventPublisher eventos;

    private final RelojDePrueba reloj = new RelojDePrueba(INICIO, ZonaHoraria.LIMA);
    private long citaAna;
    private long citaBeto;
    private long pacienteAna;

    private ProgramarRecordatoriosUseCase programar;
    private EntregarRecordatoriosUseCase entregar;
    private ResponderRecordatorioUseCase responder;
    private LlamadasRecordatorioUseCase llamadas;
    private VinculacionTelegramUseCase vinculacion;

    @BeforeEach
    void preparar() {
        jdbc.execute("TRUNCATE recordatorio, token_vinculacion_telegram, notificacion, conversacion_chatbot, consulta, "
                + "lectura_dispositivo, documento_paciente, ciclo_tratamiento, cita, participante_estudio, medicion_registro, "
                + "paciente RESTART IDENTITY CASCADE");
        long medico = jdbc.queryForObject("SELECT MIN(id) FROM usuario", Long.class);
        pacienteAna = jdbc.queryForObject("INSERT INTO paciente (nombres, apellidos, documento_identidad, fecha_nacimiento, telefono) "
                + "VALUES ('Ana Maria', 'Rojas', '50000001', '1960-01-01', '911111111') RETURNING id", Long.class);
        long pacienteBeto = jdbc.queryForObject("INSERT INTO paciente (nombres, apellidos, documento_identidad, fecha_nacimiento, telefono) "
                + "VALUES ('Beto', 'Diaz', '50000002', '1958-01-01', '922222222') RETURNING id", Long.class);
        citaAna = jdbc.queryForObject("INSERT INTO cita (paciente_id, medico_id, fecha, hora, estado) "
                + "VALUES (?, ?, '2026-10-08', '10:30', 'PROGRAMADA') RETURNING id", Long.class, pacienteAna, medico);
        citaBeto = jdbc.queryForObject("INSERT INTO cita (paciente_id, medico_id, fecha, hora, estado) "
                + "VALUES (?, ?, '2026-10-06', '11:00', 'PROGRAMADA') RETURNING id", Long.class, pacienteBeto, medico);

        var gestor = new GestorConsultasChatbot(consultas, (ruta, payload) -> { }, reloj);
        var acciones = new AccionesCitaPacienteService(citas, eventos, reloj);
        programar = new ProgramarRecordatoriosUseCase(citas, pacientes, recordatorios, reloj);
        entregar = new EntregarRecordatoriosUseCase(recordatorios, citas, pacientes, reloj);
        responder = new ResponderRecordatorioUseCase(recordatorios, pacientes, acciones, gestor,
                new FrontendProperties("https://portal.test"), reloj);
        llamadas = new LlamadasRecordatorioUseCase(recordatorios, citas, pacientes, eventos, reloj);
        vinculacion = new VinculacionTelegramUseCase(pacientes, tokens, generadorToken,
                new TelegramProperties("FundacionBot", 30), eventos, reloj);
    }

    private void vincularAna() {
        String token = generadorToken.generarToken();
        jdbc.update("INSERT INTO token_vinculacion_telegram (paciente_id, token_hash, expira_en) VALUES (?, ?, ?::timestamptz)",
                pacienteAna, generadorToken.hash(token), "2026-10-05T16:00:00Z");
        tx.executeWithoutResult(s -> vinculacion.vincular(token, CHAT_ANA));
    }

    @Test
    void elTokenDeVinculacionEsDeUnSoloUso() {
        String token = generadorToken.generarToken();
        jdbc.update("INSERT INTO token_vinculacion_telegram (paciente_id, token_hash, expira_en) VALUES (?, ?, ?::timestamptz)",
                pacienteAna, generadorToken.hash(token), "2026-10-05T16:00:00Z");

        tx.executeWithoutResult(s -> vinculacion.vincular(token, CHAT_ANA));

        assertThat(pacientes.buscarPorTelegramChatId(CHAT_ANA)).get().extracting(p -> p.getId()).isEqualTo(pacienteAna);
        assertThatThrownBy(() -> tx.executeWithoutResult(s -> vinculacion.vincular(token, 999L)))
                .hasMessageContaining("ya fue usado");
    }

    @Test
    void cicloCompletoDeRecordatoriosPorTelegramYPorLlamada() {
        vincularAna();

        Integer creados = tx.execute(s -> programar.ejecutar());
        assertThat(creados).isEqualTo(4); // Ana: 72h, 24h y 2h por Telegram; Beto (sin Telegram): llamada 24h
        Integer segundaVez = tx.execute(s -> programar.ejecutar());
        assertThat(segundaVez).isZero(); // idempotente

        // 11:00 Lima: vence el aviso de 72 h de Ana (5/10 10:30) y la llamada a Beto (5/10 11:00)
        reloj.fijar(Instant.parse("2026-10-05T16:00:00Z"));
        List<RecordatorioParaEnviar> lote = tx.execute(s -> entregar.tomarPendientes(50));
        assertThat(lote).singleElement().satisfies(r -> {
            assertThat(r.chatId()).isEqualTo(CHAT_ANA);
            assertThat(r.texto()).startsWith("Hola Ana 👋").contains("10:30").doesNotContain("Rojas");
        });
        List<RecordatorioParaEnviar> reintento = tx.execute(s -> entregar.tomarPendientes(50));
        assertThat(reintento).isEmpty(); // ya tomado: no se reenvia

        tx.executeWithoutResult(s -> entregar.registrarResultado(lote.getFirst().recordatorioId(), true, "msg-1", null));
        assertThat(recordatorios.buscarPorId(lote.getFirst().recordatorioId())).get()
                .extracting(r -> r.getEstado()).isEqualTo(EstadoRecordatorio.ENVIADO);

        String respuesta = tx.execute(s -> responder.responder(CHAT_ANA, lote.getFirst().recordatorioId(), AccionRecordatorio.CONFIRMAR));
        assertThat(respuesta).contains("confirmada");
        assertThat(citas.buscarPorId(citaAna)).get().extracting(c -> c.getEstado()).isEqualTo(EstadoCita.CONFIRMADA);

        var paraLlamar = tx.execute(s -> llamadas.pendientes());
        assertThat(paraLlamar).singleElement().satisfies(l -> {
            assertThat(l.citaId()).isEqualTo(citaBeto);
            assertThat(l.telefono()).isEqualTo("922222222");
        });
    }

    @Test
    void unChatAjenoNoPuedeResponderElRecordatorioDeOtroPaciente() {
        vincularAna();
        tx.execute(s -> programar.ejecutar());
        reloj.fijar(Instant.parse("2026-10-05T16:00:00Z"));
        Long avisoId = tx.execute(s -> entregar.tomarPendientes(50)).getFirst().recordatorioId();

        assertThatThrownBy(() -> tx.execute(s -> responder.responder(123L, avisoId, AccionRecordatorio.CANCELAR)))
                .hasMessageContaining("no encontrado");
        assertThat(citas.buscarPorId(citaAna)).get().extracting(c -> c.getEstado()).isEqualTo(EstadoCita.PROGRAMADA);
    }

    @Test
    void siLaCitaSeReprogramaLosAvisosViejosSeCancelanEnVezDeEnviarse() {
        vincularAna();
        tx.execute(s -> programar.ejecutar());
        tx.executeWithoutResult(s -> {
            var cita = citas.buscarPorId(citaAna).orElseThrow();
            cita.reprogramar(LocalDate.of(2026, 10, 15), LocalTime.of(9, 0));
            citas.guardar(cita);
        });

        reloj.avanzar(Duration.ofDays(3)); // vencen los avisos calculados para la fecha anterior
        List<RecordatorioParaEnviar> vencidos = tx.execute(s -> entregar.tomarPendientes(50));
        assertThat(vencidos).isEmpty();
        assertThat(recordatorios.listarPorCita(citaAna)).extracting(r -> r.getEstado())
                .containsOnly(EstadoRecordatorio.CANCELADO);
    }

    @Test
    void cancelarDesdeTelegramSacaLaCitaDelDenominadorDelTns() {
        vincularAna();
        tx.execute(s -> programar.ejecutar());
        reloj.fijar(Instant.parse("2026-10-05T16:00:00Z"));
        Long avisoId = tx.execute(s -> entregar.tomarPendientes(50)).getFirst().recordatorioId();

        tx.execute(s -> responder.responder(CHAT_ANA, avisoId, AccionRecordatorio.CANCELAR));

        assertThat(citas.buscarPorId(citaAna)).get().extracting(c -> c.getEstado()).isEqualTo(EstadoCita.CANCELADA);
        assertThat(recordatorios.buscarPorId(avisoId)).get().extracting(r -> r.getRespuesta().name()).isEqualTo("CANCELO");
    }
}

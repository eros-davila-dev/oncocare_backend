package com.threepartners.oncologia.application.telegram;

import com.threepartners.oncologia.application.chatbot.GestorConsultasChatbot;
import com.threepartners.oncologia.application.cita.AccionesCitaPacienteService;
import com.threepartners.oncologia.application.recordatorio.EntregarRecordatoriosUseCase;
import com.threepartners.oncologia.application.recordatorio.EntregarRecordatoriosUseCase.RecordatorioParaEnviar;
import com.threepartners.oncologia.application.recordatorio.ProgramarRecordatoriosUseCase;
import com.threepartners.oncologia.application.recordatorio.ResponderRecordatorioUseCase;
import com.threepartners.oncologia.application.recordatorio.ResponderRecordatorioUseCase.AccionRecordatorio;
import com.threepartners.oncologia.config.FrontendProperties;
import com.threepartners.oncologia.config.RecordatoriosProperties;
import com.threepartners.oncologia.config.TelegramProperties;
import com.threepartners.oncologia.domain.cita.CitaRepositoryPort;
import com.threepartners.oncologia.domain.estudio.ConsultaRepositoryPort;
import com.threepartners.oncologia.domain.paciente.PacienteRepositoryPort;
import com.threepartners.oncologia.domain.paciente.TokenVinculacionTelegramRepositoryPort;
import com.threepartners.oncologia.domain.paciente.VinculacionTelegramPendienteRepositoryPort;
import com.threepartners.oncologia.domain.recordatorio.RecordatorioRepositoryPort;
import com.threepartners.oncologia.domain.shared.ZonaHoraria;
import com.threepartners.oncologia.domain.shared.exception.RecursoNoEncontradoException;
import com.threepartners.oncologia.domain.usuario.GeneradorTokenPort;
import com.threepartners.oncologia.soporte.PostgresIntegracionTest;
import com.threepartners.oncologia.soporte.RelojDePrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Vinculacion de Telegram con "Compartir mi numero" (paciente y referido) y
 * los casos reales que la motivan, contra PostgreSQL: numero escrito de otra
 * forma, contacto ajeno, grupo, referido sin autorizacion, digitos del DNI
 * equivocados, recordatorios y botones del referido, y /stop del referido.
 */
class VinculacionTelegramPorTelefonoIntegracionTest extends PostgresIntegracionTest {

    /** 5/10/2026 10:00 en Lima. */
    private static final Instant INICIO = Instant.parse("2026-10-05T15:00:00Z");
    private static final long CHAT_ANA = 111_000L;
    private static final long CHAT_LUCIA = 222_000L;
    private static final long CHAT_EXTRANO = 333_000L;

    @Autowired private JdbcTemplate jdbc;
    @Autowired private TransactionTemplate tx;
    @Autowired private CitaRepositoryPort citas;
    @Autowired private PacienteRepositoryPort pacientes;
    @Autowired private RecordatorioRepositoryPort recordatorios;
    @Autowired private VinculacionTelegramPendienteRepositoryPort pendientes;
    @Autowired private TokenVinculacionTelegramRepositoryPort tokens;
    @Autowired private ConsultaRepositoryPort consultas;
    @Autowired private GeneradorTokenPort generadorToken;
    @Autowired private ApplicationEventPublisher eventos;

    private final RelojDePrueba reloj = new RelojDePrueba(INICIO, ZonaHoraria.LIMA);
    private long pacienteAna;
    private long pacienteBeto;
    private VinculacionTelegramPorTelefonoUseCase porTelefono;
    private VinculacionTelegramUseCase vinculacion;
    private ProgramarRecordatoriosUseCase programar;
    private EntregarRecordatoriosUseCase entregar;
    private ResponderRecordatorioUseCase responder;

    @BeforeEach
    void preparar() {
        jdbc.execute("TRUNCATE telegram_vinculacion_pendiente, recordatorio, token_vinculacion_telegram, notificacion, "
                + "conversacion_chatbot, consulta, lectura_dispositivo, documento_paciente, ciclo_tratamiento, cita, "
                + "participante_estudio, medicion_registro, paciente RESTART IDENTITY CASCADE");
        long medico = jdbc.queryForObject("SELECT MIN(id) FROM usuario", Long.class);
        // Ana autorizo que su hija Lucia reciba los recordatorios; Beto no autorizo a su referido.
        pacienteAna = jdbc.queryForObject("INSERT INTO paciente (nombres, apellidos, documento_identidad, fecha_nacimiento, "
                + "telefono, contacto_emergencia_nombre, contacto_emergencia_telefono, contacto_recibe_recordatorios) "
                + "VALUES ('Ana Maria', 'Rojas', '50000123', '1960-01-01', '911 111 111', 'Lucia Rojas', '+51 933-333-333', TRUE) "
                + "RETURNING id", Long.class);
        pacienteBeto = jdbc.queryForObject("INSERT INTO paciente (nombres, apellidos, documento_identidad, fecha_nacimiento, "
                + "telefono, contacto_emergencia_nombre, contacto_emergencia_telefono, contacto_recibe_recordatorios) "
                + "VALUES ('Beto', 'Diaz', '50000456', '1958-01-01', '922222222', 'Carla Diaz', '944444444', FALSE) "
                + "RETURNING id", Long.class);
        jdbc.update("INSERT INTO cita (paciente_id, medico_id, fecha, hora, estado) VALUES (?, ?, '2026-10-08', '10:30', 'PROGRAMADA')",
                pacienteAna, medico);

        porTelefono = new VinculacionTelegramPorTelefonoUseCase(pacientes, pendientes, eventos, reloj);
        vinculacion = new VinculacionTelegramUseCase(pacientes, tokens, generadorToken,
                new TelegramProperties("FundacionBot", 30), eventos, reloj);
        programar = new ProgramarRecordatoriosUseCase(citas, pacientes, recordatorios, new RecordatoriosProperties(false), reloj);
        entregar = new EntregarRecordatoriosUseCase(recordatorios, citas, pacientes, reloj);
        var gestor = new GestorConsultasChatbot(consultas, (ruta, payload) -> { }, reloj);
        responder = new ResponderRecordatorioUseCase(recordatorios, pacientes, new AccionesCitaPacienteService(citas, eventos, reloj),
                gestor, new FrontendProperties("https://portal.test"), reloj);
    }

    private String compartir(long chat, String telefono) {
        return tx.execute(s -> porTelefono.compartioNumero(chat, chat, chat, telefono));
    }

    private String escribir(long chat, String texto) {
        return tx.execute(s -> porTelefono.confirmar(chat, texto).orElse("(al chatbot)"));
    }

    @Test
    void elPacienteSeVinculaConSuNumeroEscritoDeOtraFormaYLosDigitosDelDni() {
        assertThat(compartir(CHAT_ANA, "51911111111")).isEqualTo(VinculacionTelegramPorTelefonoUseCase.MENSAJE_PEDIR_DIGITOS);
        assertThat(escribir(CHAT_ANA, "hola")).contains("Escribe solo los 3 ultimos digitos");
        assertThat(escribir(CHAT_ANA, "999")).contains("Te quedan 2");
        assertThat(escribir(CHAT_ANA, "123")).startsWith("¡Listo, Ana!").doesNotContain("Rojas");

        assertThat(pacientes.buscarPorTelegramChatId(CHAT_ANA)).get().extracting(p -> p.getId()).isEqualTo(pacienteAna);
        assertThat(escribir(CHAT_ANA, "¿cual es mi cita?")).isEqualTo("(al chatbot)"); // ya no hay pendiente
    }

    @Test
    void elReferidoAutorizadoRecibeLosRecordatoriosYPuedeConfirmarLaCita() {
        compartir(CHAT_ANA, "911111111");
        escribir(CHAT_ANA, "123");
        compartir(CHAT_LUCIA, "933333333");
        assertThat(escribir(CHAT_LUCIA, "123")).contains("los recordatorios de las citas de Ana");

        Integer creados = tx.execute(s -> programar.ejecutar());
        assertThat(creados).isEqualTo(6); // 72h/24h/2h a Ana y a Lucia; Beto no tiene cita
        reloj.fijar(Instant.parse("2026-10-05T16:00:00Z")); // vence el aviso de 72 h
        List<RecordatorioParaEnviar> lote = tx.execute(s -> entregar.tomarPendientes(50));
        assertThat(lote).extracting(RecordatorioParaEnviar::chatId).containsExactlyInAnyOrder(CHAT_ANA, CHAT_LUCIA);
        RecordatorioParaEnviar aLucia = lote.stream().filter(r -> r.chatId() == CHAT_LUCIA).findFirst().orElseThrow();
        assertThat(aLucia.texto()).startsWith("Hola Lucia 👋").contains("Ana tiene una cita").doesNotContain("Rojas");

        String respuesta = tx.execute(s -> responder.responder(CHAT_LUCIA, aLucia.recordatorioId(), AccionRecordatorio.CONFIRMAR));
        assertThat(respuesta).contains("La cita de Ana quedo confirmada");
        assertThatThrownBy(() -> tx.execute(s -> responder.responder(CHAT_EXTRANO, aLucia.recordatorioId(), AccionRecordatorio.CANCELAR)))
                .isInstanceOf(RecursoNoEncontradoException.class);

        // El /stop de Lucia no apaga los recordatorios de Ana.
        Boolean desvinculado = tx.execute(s -> vinculacion.desvincularPorChat(CHAT_LUCIA));
        assertThat(desvinculado).isTrue();
        assertThat(pacientes.buscarPorId(pacienteAna)).get().satisfies(p -> {
            assertThat(p.getContactoTelegramChatId()).isNull();
            assertThat(p.getTelegramChatId()).isEqualTo(CHAT_ANA);
        });
    }

    @Test
    void rechazaContactosAjenosGruposYNumerosDesconocidos() {
        String ajeno = tx.execute(s -> porTelefono.compartioNumero(CHAT_EXTRANO, CHAT_EXTRANO, 999L, "911111111"));
        assertThat(ajeno).isEqualTo(VinculacionTelegramPorTelefonoUseCase.MENSAJE_SOLO_PROPIO);
        String grupo = tx.execute(s -> porTelefono.compartioNumero(-100L, -100L, -100L, "911111111"));
        assertThat(grupo).isEqualTo(VinculacionTelegramPorTelefonoUseCase.MENSAJE_SOLO_PRIVADO);
        assertThat(compartir(CHAT_EXTRANO, "955555555")).isEqualTo(VinculacionTelegramPorTelefonoUseCase.MENSAJE_NO_ENCONTRADO);
        // El referido de Beto no fue autorizado.
        assertThat(compartir(CHAT_EXTRANO, "944444444")).isEqualTo(VinculacionTelegramPorTelefonoUseCase.MENSAJE_SIN_AUTORIZACION);
    }

    @Test
    void tresIntentosFallidosBloqueanElChatSinVincular() {
        compartir(CHAT_EXTRANO, "911111111"); // numero de Ana, pero quien escribe no sabe su DNI
        escribir(CHAT_EXTRANO, "000");
        escribir(CHAT_EXTRANO, "111");
        assertThat(escribir(CHAT_EXTRANO, "222")).contains("espera 30 minutos");

        assertThat(compartir(CHAT_EXTRANO, "911111111")).isEqualTo(VinculacionTelegramPorTelefonoUseCase.MENSAJE_BLOQUEADO);
        assertThat(pacientes.buscarPorId(pacienteAna)).get().extracting(p -> p.getTelegramChatId()).isNull();
        assertThat(pacientes.buscarPorId(pacienteBeto)).get().extracting(p -> p.getTelegramChatId()).isNull();
    }
}

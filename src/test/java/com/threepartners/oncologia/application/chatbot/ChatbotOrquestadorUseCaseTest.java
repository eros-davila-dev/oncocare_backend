package com.threepartners.oncologia.application.chatbot;

import com.threepartners.oncologia.application.cita.AgendarCitaUseCase;
import com.threepartners.oncologia.application.cita.CancelarCitaUseCase;
import com.threepartners.oncologia.application.cita.ConsultarCitaUseCase;
import com.threepartners.oncologia.application.cita.RegistrarAsistenciaCitaUseCase;
import com.threepartners.oncologia.application.cita.ReprogramarCitaUseCase;
import com.threepartners.oncologia.config.FrontendProperties;
import com.threepartners.oncologia.config.RegistroProperties;
import com.threepartners.oncologia.domain.chatbot.ConversacionChatbotRepositoryPort;
import com.threepartners.oncologia.domain.chatbot.GeminiPort;
import com.threepartners.oncologia.domain.chatbot.Intencion;
import com.threepartners.oncologia.domain.chatbot.InterpretacionChatbot;
import com.threepartners.oncologia.domain.chatbot.PreguntaFrecuenteRepositoryPort;
import com.threepartners.oncologia.domain.chatbot.ResultadoAccion;
import com.threepartners.oncologia.domain.estudio.CanalConsulta;
import com.threepartners.oncologia.domain.estudio.Consulta;
import com.threepartners.oncologia.domain.cita.Cita;
import com.threepartners.oncologia.domain.cita.EstadoCita;
import com.threepartners.oncologia.domain.paciente.Paciente;
import com.threepartners.oncologia.domain.paciente.PacienteRepositoryPort;
import com.threepartners.oncologia.domain.shared.CriterioPaginacion;
import com.threepartners.oncologia.domain.shared.Pagina;
import com.threepartners.oncologia.domain.usuario.Rol;
import com.threepartners.oncologia.domain.usuario.Usuario;
import com.threepartners.oncologia.domain.usuario.UsuarioRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatbotOrquestadorUseCaseTest {

    @Mock
    private GeminiPort geminiPort;
    @Mock
    private ConversacionChatbotRepositoryPort conversacionChatbotRepositoryPort;
    @Mock
    private PacienteRepositoryPort pacienteRepositoryPort;
    @Mock
    private UsuarioRepositoryPort usuarioRepositoryPort;
    @Mock
    private ConsultarCitaUseCase consultarCitaUseCase;
    @Mock
    private AgendarCitaUseCase agendarCitaUseCase;
    @Mock
    private ReprogramarCitaUseCase reprogramarCitaUseCase;
    @Mock
    private CancelarCitaUseCase cancelarCitaUseCase;
    @Mock
    private RegistrarAsistenciaCitaUseCase registrarAsistenciaCitaUseCase;
    @Mock
    private GestorConsultasChatbot gestorConsultas;
    @Mock
    private PreguntaFrecuenteRepositoryPort preguntaFrecuenteRepositoryPort;

    private ChatbotOrquestadorUseCase useCase;

    @BeforeEach
    void setUp() {
        FrontendProperties frontendProperties = new FrontendProperties("http://localhost:4200");
        RegistroProperties registroProperties = new RegistroProperties(true);
        useCase = new ChatbotOrquestadorUseCase(
                geminiPort, conversacionChatbotRepositoryPort, pacienteRepositoryPort, usuarioRepositoryPort,
                consultarCitaUseCase, agendarCitaUseCase, reprogramarCitaUseCase, cancelarCitaUseCase,
                registrarAsistenciaCitaUseCase, frontendProperties, registroProperties, gestorConsultas,
                preguntaFrecuenteRepositoryPort, Clock.systemUTC());
        when(conversacionChatbotRepositoryPort.listarPorSesion(anyString(), any(Integer.class))).thenReturn(List.of());
        lenient().when(gestorConsultas.registrarTurno(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Consulta.builder().id(1L).build());
    }

    private Paciente pacienteDePrueba() {
        return Paciente.builder().id(10L).usuarioId(5L).nombres("Ana").apellidos("Torres").build();
    }

    @Test
    void unVisitanteAnonimoQuePideConsultarCitasEsRedirigidoALoginSinLlamarAlBackendDeNegocio() {
        when(geminiPort.interpretar(anyString(), any(), any()))
                .thenReturn(new InterpretacionChatbot(Intencion.CHECK_APPOINTMENT, Map.of(), false, "..."));

        String respuesta = useCase.procesar("sesion-1", "¿cuando es mi cita?", CanalConsulta.CHATBOT_WEB, null, null, "127.0.0.1").texto();

        assertThat(respuesta).contains("iniciar sesion");
        verifyNoInteractions(consultarCitaUseCase);
    }

    @Test
    void unaConsultaGeneralDevuelveDirectamenteLaRespuestaDeGemini() {
        when(geminiPort.interpretar(anyString(), any(), any()))
                .thenReturn(new InterpretacionChatbot(Intencion.GENERAL_QUERY, Map.of(), true, "Puedes registrarte en el portal."));

        String respuesta = useCase.procesar("sesion-2", "¿como me registro?", CanalConsulta.CHATBOT_WEB, null, null, "127.0.0.1").texto();

        assertThat(respuesta).isEqualTo("Puedes registrarte en el portal.");
    }

    @Test
    void unPacienteAutenticadoSinCitasProximasRecibeUnaRespuestaClara() {
        Paciente paciente = pacienteDePrueba();
        when(pacienteRepositoryPort.buscarPorUsuarioId(5L)).thenReturn(Optional.of(paciente));
        when(usuarioRepositoryPort.buscarPorId(5L)).thenReturn(Optional.of(Usuario.builder().id(5L).nombres("Ana").build()));
        when(geminiPort.interpretar(anyString(), any(), any()))
                .thenReturn(new InterpretacionChatbot(Intencion.CHECK_APPOINTMENT, Map.of(), true, "..."));
        when(consultarCitaUseCase.misCitas(5L, null, CriterioPaginacion.de(0, 5)))
                .thenReturn(new Pagina<>(List.of(), 0, 0, 0, 5));

        String respuesta = useCase.procesar("sesion-3", "¿tengo alguna cita?", CanalConsulta.CHATBOT_WEB, 5L, Rol.PACIENTE, "127.0.0.1").texto();

        assertThat(respuesta).contains("No tienes citas proximas");
    }

    @Test
    void unPacienteAutenticadoConCitaProximaRecibeLaFechaReal() {
        Paciente paciente = pacienteDePrueba();
        Cita cita = Cita.builder().id(1L).pacienteId(10L).medicoId(2L)
                .fecha(LocalDate.of(2026, 10, 1)).hora(LocalTime.of(9, 30))
                .tipoConsulta("Control").estado(EstadoCita.PROGRAMADA).build();

        when(pacienteRepositoryPort.buscarPorUsuarioId(5L)).thenReturn(Optional.of(paciente));
        when(usuarioRepositoryPort.buscarPorId(5L)).thenReturn(Optional.of(Usuario.builder().id(5L).nombres("Ana").build()));
        when(geminiPort.interpretar(anyString(), any(), any()))
                .thenReturn(new InterpretacionChatbot(Intencion.CHECK_APPOINTMENT, Map.of(), true, "..."));
        when(consultarCitaUseCase.misCitas(5L, null, CriterioPaginacion.de(0, 5)))
                .thenReturn(new Pagina<>(List.of(cita), 1, 1, 0, 5));

        String respuesta = useCase.procesar("sesion-4", "¿cuando es mi proxima cita?", CanalConsulta.CHATBOT_WEB, 5L, Rol.PACIENTE, "127.0.0.1").texto();

        assertThat(respuesta).contains("2026-10-01").contains("09:30");
    }

    @Test
    void registraLaConversacionConLaIntencionDetectada() {
        when(geminiPort.interpretar(anyString(), any(), any()))
                .thenReturn(new InterpretacionChatbot(Intencion.HELP, Map.of(), true, "Puedo ayudarte con tus citas."));

        useCase.procesar("sesion-5", "ayuda", CanalConsulta.CHATBOT_WEB, null, null, "127.0.0.1");

        verify(conversacionChatbotRepositoryPort).guardar(
                org.mockito.ArgumentMatchers.argThat(c -> c != null && "HELP".equals(c.getIntencionDetectada())));
    }

    @Test
    void informaAlGestorDeConsultasQueLaAccionSeEjecutoParaElIndicadorNca() {
        Paciente paciente = pacienteDePrueba();
        when(pacienteRepositoryPort.buscarPorUsuarioId(5L)).thenReturn(Optional.of(paciente));
        when(usuarioRepositoryPort.buscarPorId(5L)).thenReturn(Optional.of(Usuario.builder().id(5L).nombres("Ana").build()));
        when(geminiPort.interpretar(anyString(), any(), any()))
                .thenReturn(new InterpretacionChatbot(Intencion.CHECK_APPOINTMENT, Map.of(), true, "..."));
        when(consultarCitaUseCase.misCitas(5L, null, CriterioPaginacion.de(0, 5)))
                .thenReturn(new Pagina<>(List.of(), 0, 0, 0, 5));

        useCase.procesar("sesion-6", "mis citas", CanalConsulta.CHATBOT_WEB, 5L, Rol.PACIENTE, "127.0.0.1");

        verify(gestorConsultas).registrarTurno(eq("sesion-6"), eq(CanalConsulta.CHATBOT_WEB), eq(10L),
                eq(Intencion.CHECK_APPOINTMENT), eq(ResultadoAccion.EXITO), eq("mis citas"), any());
    }

    @Test
    void sinSesionUnaAccionSobreCitasQuedaComoRequiereSesionYNoComoResuelta() {
        when(geminiPort.interpretar(anyString(), any(), any()))
                .thenReturn(new InterpretacionChatbot(Intencion.BOOK_APPOINTMENT, Map.of(), false, "..."));

        useCase.procesar("sesion-7", "quiero una cita", CanalConsulta.CHATBOT_WEB, null, null, "127.0.0.1");

        verify(gestorConsultas).registrarTurno(eq("sesion-7"), any(), eq(null), eq(Intencion.BOOK_APPOINTMENT),
                eq(ResultadoAccion.REQUIERE_SESION), any(), any());
    }

    @Test
    void unaPreguntaMedicaSeEscalaAlPersonal() {
        when(geminiPort.interpretar(anyString(), any(), any()))
                .thenReturn(new InterpretacionChatbot(Intencion.ESCALATE_TO_STAFF, Map.of(), false, "El equipo medico te respondera."));

        useCase.procesar("sesion-8", "me duele despues de la quimio", CanalConsulta.CHATBOT_WEB, null, null, "127.0.0.1");

        verify(gestorConsultas).registrarTurno(eq("sesion-8"), any(), any(), eq(Intencion.ESCALATE_TO_STAFF),
                eq(ResultadoAccion.ESCALAR), any(), any());
    }
}

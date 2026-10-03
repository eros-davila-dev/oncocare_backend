package com.threepartners.oncologia.application.chatbot;

import com.threepartners.oncologia.application.cita.AccionesCitaPacienteService;
import com.threepartners.oncologia.application.cita.AgendarCitaUseCase;
import com.threepartners.oncologia.application.cita.CancelarCitaUseCase;
import com.threepartners.oncologia.application.cita.ConsultarCitaUseCase;
import com.threepartners.oncologia.application.cita.RegistrarAsistenciaCitaUseCase;
import com.threepartners.oncologia.application.cita.ReprogramarCitaUseCase;
import com.threepartners.oncologia.config.FrontendProperties;
import com.threepartners.oncologia.config.RegistroProperties;
import com.threepartners.oncologia.domain.chatbot.ContextoUsuarioChatbot;
import com.threepartners.oncologia.domain.chatbot.ConversacionChatbot;
import com.threepartners.oncologia.domain.chatbot.ConversacionChatbotRepositoryPort;
import com.threepartners.oncologia.domain.chatbot.GeminiPort;
import com.threepartners.oncologia.domain.chatbot.Intencion;
import com.threepartners.oncologia.domain.chatbot.InterpretacionChatbot;
import com.threepartners.oncologia.domain.chatbot.PreguntaFrecuenteRepositoryPort;
import com.threepartners.oncologia.domain.chatbot.RespuestaAccion;
import com.threepartners.oncologia.domain.chatbot.RespuestaChatbot;
import com.threepartners.oncologia.domain.chatbot.ResultadoAccion;
import com.threepartners.oncologia.domain.cita.Cita;
import com.threepartners.oncologia.domain.cita.EstadoCita;
import com.threepartners.oncologia.domain.cita.OrigenCita;
import com.threepartners.oncologia.domain.estudio.CanalConsulta;
import com.threepartners.oncologia.domain.estudio.Consulta;
import com.threepartners.oncologia.domain.paciente.Paciente;
import com.threepartners.oncologia.domain.paciente.PacienteRepositoryPort;
import com.threepartners.oncologia.domain.shared.CriterioPaginacion;
import com.threepartners.oncologia.domain.shared.exception.DomainException;
import com.threepartners.oncologia.domain.usuario.Especialidad;
import com.threepartners.oncologia.domain.usuario.Rol;
import com.threepartners.oncologia.domain.usuario.Usuario;
import com.threepartners.oncologia.domain.usuario.UsuarioRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.threepartners.oncologia.domain.chatbot.ResultadoAccion.ERROR_NEGOCIO;
import static com.threepartners.oncologia.domain.chatbot.ResultadoAccion.ESCALAR;
import static com.threepartners.oncologia.domain.chatbot.ResultadoAccion.EXITO;
import static com.threepartners.oncologia.domain.chatbot.ResultadoAccion.INFORMATIVA;
import static com.threepartners.oncologia.domain.chatbot.ResultadoAccion.REQUIERE_DATOS;
import static com.threepartners.oncologia.domain.chatbot.ResultadoAccion.REQUIERE_SESION;

/**
 * Punto unico de orquestacion del chatbot (seccion 13): Gemini solo
 * interpreta el mensaje (GeminiPort); esta clase es quien decide, con datos
 * reales de la base de datos y las reglas de negocio ya existentes (los mismos
 * casos de uso que usa el resto del sistema), que accion ejecutar. Gemini
 * jamas toca la base de datos ni ejecuta nada por si mismo.
 *
 * Cada accion informa un {@link ResultadoAccion} (lo que realmente paso) y
 * {@link GestorConsultasChatbot} lo traduce al desenlace de la consulta que
 * mide el indicador NCA.
 */
@Service
@RequiredArgsConstructor
public class ChatbotOrquestadorUseCase {

    private static final int HISTORIAL_MAXIMO = 10;
    private static final String MENSAJE_REQUIERE_SESION =
            "Para gestionar tus citas necesitas iniciar sesion con tu cuenta de paciente. "
                    + "Si aun no tienes una, puedes crearla en /auth/registro.";
    private static final String MENSAJE_REQUIERE_PERFIL =
            "Antes de gestionar tus citas necesitas completar tu perfil de paciente (seccion \"Completar perfil\").";
    private static final String MENSAJE_VINCULAR_TELEGRAM =
            "Para consultar o gestionar tus citas por aqui, vincula tu Telegram desde el portal (Mi perfil) "
                    + "o pide el codigo QR en recepcion. Mientras tanto puedo responder tus preguntas generales.";
    private static final String MENSAJE_REGISTRO_NO_DISPONIBLE =
            "El registro de nuevas cuentas de paciente estara disponible proximamente. Por ahora, contacta directamente a la fundacion.";

    private final GeminiPort geminiPort;
    private final ConversacionChatbotRepositoryPort conversacionChatbotRepositoryPort;
    private final PacienteRepositoryPort pacienteRepositoryPort;
    private final UsuarioRepositoryPort usuarioRepositoryPort;
    private final ConsultarCitaUseCase consultarCitaUseCase;
    private final AgendarCitaUseCase agendarCitaUseCase;
    private final ReprogramarCitaUseCase reprogramarCitaUseCase;
    private final CancelarCitaUseCase cancelarCitaUseCase;
    private final RegistrarAsistenciaCitaUseCase registrarAsistenciaCitaUseCase;
    private final FrontendProperties frontendProperties;
    private final RegistroProperties registroProperties;
    private final GestorConsultasChatbot gestorConsultas;
    private final PreguntaFrecuenteRepositoryPort preguntaFrecuenteRepositoryPort;
    private final Clock clock;
    private final AccionesCitaPacienteService accionesCitaPacienteService;

    /**
     * Deliberadamente sin @Transactional: la llamada a Gemini es una peticion
     * HTTP externa potencialmente lenta y no debe mantener abierta una
     * conexion de base de datos mientras espera. Los casos de uso que si
     * escriben en la base (agendar/cancelar/reprogramar/confirmar) ya tienen
     * su propia demarcacion transaccional.
     */
    public RespuestaChatbot procesar(String sesionId, String mensajeUsuario, CanalConsulta canal,
                                     Long usuarioAutenticadoId, Rol rolAutenticado, String ipOrigen) {
        Paciente paciente = resolverPaciente(usuarioAutenticadoId, rolAutenticado);
        ContextoUsuarioChatbot contexto = construirContexto(usuarioAutenticadoId, rolAutenticado, paciente);
        return procesarTurno(sesionId, mensajeUsuario, canal, contexto,
                interpretacion -> ejecutarAccion(interpretacion, paciente, usuarioAutenticadoId, rolAutenticado, ipOrigen),
                paciente);
    }

    /**
     * Mensaje recibido por Telegram (via n8n). La identidad no viene de un
     * JWT sino del chat vinculado: si el chat no esta vinculado, el bot
     * responde preguntas generales e invita a vincularlo.
     */
    public RespuestaChatbot procesarDesdeTelegram(Long chatId, String mensajeUsuario) {
        Paciente paciente = pacienteRepositoryPort.buscarPorTelegramChatId(chatId).orElse(null);
        ContextoUsuarioChatbot contexto = paciente != null
                ? new ContextoUsuarioChatbot(true, true, paciente.nombrePila())
                : new ContextoUsuarioChatbot(false, false, null);
        return procesarTurno("tg-" + chatId, mensajeUsuario, CanalConsulta.TELEGRAM, contexto,
                interpretacion -> ejecutarAccionTelegram(interpretacion, paciente), paciente);
    }

    private RespuestaChatbot procesarTurno(String sesionId, String mensajeUsuario, CanalConsulta canal,
                                           ContextoUsuarioChatbot contextoUsuario,
                                           Function<InterpretacionChatbot, RespuestaAccion> ejecutor,
                                           Paciente paciente) {
        Instant inicioTurno = clock.instant();
        List<ConversacionChatbot> historial = conversacionChatbotRepositoryPort.listarPorSesion(sesionId, HISTORIAL_MAXIMO);
        ContextoUsuarioChatbot contexto = contextoUsuario.conConocimiento(preguntaFrecuenteRepositoryPort.listarActivas());

        InterpretacionChatbot interpretacion = geminiPort.interpretar(mensajeUsuario, historial, contexto);

        RespuestaAccion respuesta = ejecutor.apply(interpretacion);

        CanalConsulta canalEfectivo = canal != null ? canal : CanalConsulta.CHATBOT_WEB;
        // Un mensaje fuera de alcance se responde pero no es una consulta: no entra al NCA.
        Consulta consulta = interpretacion.intencion() == Intencion.OUT_OF_SCOPE ? null
                : gestorConsultas.registrarTurno(sesionId, canalEfectivo, paciente != null ? paciente.getId() : null,
                        interpretacion.intencion(), respuesta.resultado(), mensajeUsuario, inicioTurno,
                        interpretacion.categoria());

        conversacionChatbotRepositoryPort.guardar(ConversacionChatbot.builder()
                .pacienteId(paciente != null ? paciente.getId() : null)
                .sesionId(sesionId)
                .consultaId(consulta != null ? consulta.getId() : null)
                .mensajeUsuario(mensajeUsuario)
                .respuestaBot(respuesta.texto())
                .intencionDetectada(interpretacion.intencion().name())
                .canal(canalEfectivo.name())
                .fecha(clock.instant())
                .build());

        return consulta == null ? new RespuestaChatbot(respuesta.texto(), null, null)
                : new RespuestaChatbot(respuesta.texto(), consulta.getId(), consulta.getResultado());
    }

    private Paciente resolverPaciente(Long usuarioAutenticadoId, Rol rolAutenticado) {
        if (usuarioAutenticadoId == null || rolAutenticado != Rol.PACIENTE) {
            return null;
        }
        return pacienteRepositoryPort.buscarPorUsuarioId(usuarioAutenticadoId).orElse(null);
    }

    private ContextoUsuarioChatbot construirContexto(Long usuarioAutenticadoId, Rol rolAutenticado, Paciente paciente) {
        if (usuarioAutenticadoId == null) {
            return new ContextoUsuarioChatbot(false, false, null);
        }
        if (rolAutenticado != Rol.PACIENTE) {
            return new ContextoUsuarioChatbot(true, false, null);
        }
        String nombre = usuarioRepositoryPort.buscarPorId(usuarioAutenticadoId).map(Usuario::getNombres).orElse(null);
        return new ContextoUsuarioChatbot(true, paciente != null, nombre);
    }

    private RespuestaAccion ejecutarAccion(InterpretacionChatbot interpretacion, Paciente paciente, Long usuarioAutenticadoId, Rol rolAutenticado, String ipOrigen) {
        return switch (interpretacion.intencion()) {
            case GENERAL_QUERY, HELP -> informativa(interpretacion);
            case OUT_OF_SCOPE -> new RespuestaAccion(interpretacion.respuestaSugerida(), INFORMATIVA);
            case ESCALATE_TO_STAFF -> new RespuestaAccion(interpretacion.respuestaSugerida(), ESCALAR);
            case REGISTER_PATIENT -> registroProperties.pacientesHabilitado()
                    ? new RespuestaAccion("Puedes crear tu cuenta de paciente en " + frontendProperties.baseUrl()
                            + "/auth/registro. Solo toma un par de minutos y luego podras agendar tus citas desde aqui.", INFORMATIVA)
                    : new RespuestaAccion(MENSAJE_REGISTRO_NO_DISPONIBLE, ESCALAR);
            case CHECK_APPOINTMENT -> requierePaciente(paciente, usuarioAutenticadoId, rolAutenticado, () -> consultarCitas(paciente));
            case CONFIRM_APPOINTMENT -> requierePaciente(paciente, usuarioAutenticadoId, rolAutenticado, () -> confirmarProximaCita(paciente, usuarioAutenticadoId, rolAutenticado));
            case CANCEL_APPOINTMENT -> requierePaciente(paciente, usuarioAutenticadoId, rolAutenticado, () -> cancelarProximaCita(interpretacion, paciente, usuarioAutenticadoId, rolAutenticado, ipOrigen));
            case RESCHEDULE_APPOINTMENT -> requierePaciente(paciente, usuarioAutenticadoId, rolAutenticado, () -> reprogramarProximaCita(interpretacion, paciente, usuarioAutenticadoId, rolAutenticado, ipOrigen));
            case BOOK_APPOINTMENT -> requierePaciente(paciente, usuarioAutenticadoId, rolAutenticado, () -> agendarCita(interpretacion, paciente, usuarioAutenticadoId, rolAutenticado, ipOrigen));
        };
    }

    /**
     * Mismas reglas que en la web, ejecutadas sobre el paciente del chat
     * vinculado. Agendar y reprogramar requieren elegir medico y horario:
     * se derivan a recepcion en vez de resolverse a medias por mensaje.
     */
    private RespuestaAccion ejecutarAccionTelegram(InterpretacionChatbot interpretacion, Paciente paciente) {
        return switch (interpretacion.intencion()) {
            case GENERAL_QUERY, HELP -> informativa(interpretacion);
            case OUT_OF_SCOPE -> new RespuestaAccion(interpretacion.respuestaSugerida(), INFORMATIVA);
            case ESCALATE_TO_STAFF -> new RespuestaAccion(interpretacion.respuestaSugerida(), ESCALAR);
            case REGISTER_PATIENT -> new RespuestaAccion("Puedes crear tu cuenta de paciente en " + frontendProperties.baseUrl()
                    + "/auth/registro o pedir ayuda en recepcion.", INFORMATIVA);
            default -> {
                if (paciente == null) {
                    yield new RespuestaAccion(MENSAJE_VINCULAR_TELEGRAM, REQUIERE_SESION);
                }
                try {
                    yield accionSobreCitaTelegram(interpretacion, paciente);
                } catch (DomainException e) {
                    yield new RespuestaAccion("No pude completar la accion: " + e.getMessage(), ERROR_NEGOCIO);
                }
            }
        };
    }

    /**
     * La regla "si no esta en la informacion oficial, se deriva" la aplica el
     * backend: no basta con pedirsela al modelo. Una respuesta sin respaldo
     * no resuelve la consulta del paciente (NCA, tesis v8).
     */
    private static RespuestaAccion informativa(InterpretacionChatbot interpretacion) {
        return new RespuestaAccion(interpretacion.respuestaSugerida(),
                interpretacion.respuestaConInformacionOficial() ? INFORMATIVA : ESCALAR);
    }

    private RespuestaAccion accionSobreCitaTelegram(InterpretacionChatbot interpretacion, Paciente paciente) {
        Optional<Cita> proxima = accionesCitaPacienteService.proximaActiva(paciente);
        return switch (interpretacion.intencion()) {
            case CHECK_APPOINTMENT -> proxima
                    .map(c -> new RespuestaAccion("Tu proxima cita es el %s a las %s, estado: %s."
                            .formatted(c.getFecha(), c.getHora(), etiqueta(c.getEstado())), EXITO))
                    .orElseGet(() -> new RespuestaAccion("No tienes citas proximas programadas.", EXITO));
            case CONFIRM_APPOINTMENT -> proxima
                    .map(c -> {
                        if (c.getEstado() != EstadoCita.CONFIRMADA) {
                            accionesCitaPacienteService.confirmar(c, paciente);
                        }
                        return new RespuestaAccion("Listo, tu cita del %s a las %s esta confirmada."
                                .formatted(c.getFecha(), c.getHora()), EXITO);
                    })
                    .orElseGet(() -> new RespuestaAccion("No tienes ninguna cita pendiente de confirmar.", EXITO));
            case CANCEL_APPOINTMENT -> {
                if (proxima.isEmpty()) {
                    yield new RespuestaAccion("No tienes ninguna cita proxima para cancelar.", EXITO);
                }
                Cita cita = proxima.get();
                String motivo = interpretacion.entidad("motivo");
                if (!interpretacion.listoParaEjecutar() || motivo == null) {
                    yield new RespuestaAccion("Antes de cancelar tu cita del %s a las %s, cuentame brevemente el motivo."
                            .formatted(cita.getFecha(), cita.getHora()), REQUIERE_DATOS);
                }
                accionesCitaPacienteService.cancelar(cita, paciente, motivo);
                yield new RespuestaAccion("Tu cita del %s a las %s fue cancelada. Gracias por avisarnos."
                        .formatted(cita.getFecha(), cita.getHora()), EXITO);
            }
            default -> new RespuestaAccion("Le pedi a recepcion que te contacte para coordinar la fecha y el horario. "
                    + "Tambien puedes hacerlo en " + frontendProperties.baseUrl() + "/mis-citas", ESCALAR);
        };
    }

    /**
     * Las acciones sobre citas exigen un paciente resuelto (cuenta PACIENTE
     * con ficha ya completada); distingue el mensaje segun en cual de los dos
     * pasos del autoservicio (seccion 7) se quedo el usuario.
     */
    private RespuestaAccion requierePaciente(Paciente paciente, Long usuarioAutenticadoId, Rol rolAutenticado, Supplier<RespuestaAccion> accion) {
        if (paciente == null) {
            boolean autenticadoComoPaciente = usuarioAutenticadoId != null && rolAutenticado == Rol.PACIENTE;
            return new RespuestaAccion(autenticadoComoPaciente ? MENSAJE_REQUIERE_PERFIL : MENSAJE_REQUIERE_SESION, REQUIERE_SESION);
        }
        try {
            return accion.get();
        } catch (DomainException e) {
            return new RespuestaAccion("No pude completar la accion: " + e.getMessage(), ERROR_NEGOCIO);
        }
    }

    private RespuestaAccion consultarCitas(Paciente paciente) {
        var pagina = consultarCitaUseCase.misCitas(paciente.getUsuarioId(), null, CriterioPaginacion.de(0, 5));
        List<Cita> citas = pagina.contenido().stream()
                .filter(c -> c.getEstado() == EstadoCita.PROGRAMADA || c.getEstado() == EstadoCita.CONFIRMADA)
                .sorted(Comparator.comparing(Cita::fechaHora))
                .toList();

        if (citas.isEmpty()) {
            return new RespuestaAccion("No tienes citas proximas programadas. ¿Quieres que te ayude a agendar una?", EXITO);
        }

        Cita proxima = citas.get(0);
        String base = "Tu proxima cita es el %s a las %s (%s), estado: %s.".formatted(
                proxima.getFecha(), proxima.getHora(), proxima.getTipoConsulta(), etiqueta(proxima.getEstado()));

        return new RespuestaAccion(
                citas.size() > 1 ? base + " Tienes %d citas mas programadas.".formatted(citas.size() - 1) : base, EXITO);
    }

    private RespuestaAccion confirmarProximaCita(Paciente paciente, Long usuarioAutenticadoId, Rol rolAutenticado) {
        Cita cita = proximaCitaModificable(paciente).orElse(null);
        if (cita == null) {
            return new RespuestaAccion("No tienes ninguna cita pendiente de confirmar.", EXITO);
        }
        if (cita.getEstado() == EstadoCita.CONFIRMADA) {
            return new RespuestaAccion("Tu cita del %s a las %s ya estaba confirmada.".formatted(cita.getFecha(), cita.getHora()), EXITO);
        }
        registrarAsistenciaCitaUseCase.confirmar(cita.getId(), usuarioAutenticadoId, rolAutenticado);
        return new RespuestaAccion("Listo, confirmamos tu cita del %s a las %s.".formatted(cita.getFecha(), cita.getHora()), EXITO);
    }

    private RespuestaAccion cancelarProximaCita(InterpretacionChatbot interpretacion, Paciente paciente, Long usuarioAutenticadoId, Rol rolAutenticado, String ipOrigen) {
        Cita cita = proximaCitaModificable(paciente).orElse(null);
        if (cita == null) {
            return new RespuestaAccion("No tienes ninguna cita proxima para cancelar.", EXITO);
        }
        String motivo = interpretacion.entidad("motivo");
        if (!interpretacion.listoParaEjecutar() || motivo == null) {
            return new RespuestaAccion("Antes de cancelar tu cita del %s a las %s, cuentame brevemente el motivo."
                    .formatted(cita.getFecha(), cita.getHora()), REQUIERE_DATOS);
        }
        cancelarCitaUseCase.ejecutar(cita.getId(), motivo, usuarioAutenticadoId, rolAutenticado, ipOrigen);
        return new RespuestaAccion("Tu cita del %s a las %s fue cancelada.".formatted(cita.getFecha(), cita.getHora()), EXITO);
    }

    private RespuestaAccion reprogramarProximaCita(InterpretacionChatbot interpretacion, Paciente paciente, Long usuarioAutenticadoId, Rol rolAutenticado, String ipOrigen) {
        Cita cita = proximaCitaModificable(paciente).orElse(null);
        if (cita == null) {
            return new RespuestaAccion("No tienes ninguna cita proxima para reprogramar.", EXITO);
        }

        Optional<LocalDate> fecha = parsearFecha(interpretacion.entidad("fecha"));
        Optional<LocalTime> hora = parsearHora(interpretacion.entidad("hora"));

        if (!interpretacion.listoParaEjecutar() || fecha.isEmpty() || hora.isEmpty()) {
            return new RespuestaAccion("¿Para que nueva fecha y hora quieres reprogramar tu cita del %s?"
                    .formatted(cita.getFecha()), REQUIERE_DATOS);
        }

        Cita actualizada = reprogramarCitaUseCase.ejecutar(cita.getId(), fecha.get(), hora.get(), usuarioAutenticadoId, rolAutenticado, ipOrigen);
        return new RespuestaAccion("Tu cita quedo reprogramada para el %s a las %s."
                .formatted(actualizada.getFecha(), actualizada.getHora()), EXITO);
    }

    private RespuestaAccion agendarCita(InterpretacionChatbot interpretacion, Paciente paciente, Long usuarioAutenticadoId, Rol rolAutenticado, String ipOrigen) {
        String especialidadTexto = interpretacion.entidad("especialidad");
        Optional<LocalDate> fecha = parsearFecha(interpretacion.entidad("fecha"));
        Optional<LocalTime> hora = parsearHora(interpretacion.entidad("hora"));

        if (!interpretacion.listoParaEjecutar() || especialidadTexto == null || fecha.isEmpty() || hora.isEmpty()) {
            return new RespuestaAccion(interpretacion.respuestaSugerida(), REQUIERE_DATOS);
        }

        Especialidad especialidad;
        try {
            especialidad = Especialidad.valueOf(especialidadTexto);
        } catch (IllegalArgumentException e) {
            return new RespuestaAccion("No reconozco esa especialidad. Las disponibles son oncologia clinica, oncologia quirurgica, "
                    + "radioterapia y cuidados paliativos.", REQUIERE_DATOS);
        }

        Usuario medico = usuarioRepositoryPort.listarPorEspecialidad(especialidad).stream().findFirst().orElse(null);
        if (medico == null) {
            return new RespuestaAccion("En este momento no tenemos un medico disponible en esa especialidad. "
                    + "Le pedi a recepcion que te contacte.", ESCALAR);
        }

        Cita nuevaCita = Cita.builder()
                .pacienteId(paciente.getId())
                .medicoId(medico.getId())
                .fecha(fecha.get())
                .hora(hora.get())
                .tipoConsulta(interpretacion.entidad("motivo") != null ? interpretacion.entidad("motivo") : "Consulta oncologica")
                .origen(OrigenCita.CHATBOT_WEB)
                .build();

        Cita creada = agendarCitaUseCase.ejecutar(nuevaCita, null, usuarioAutenticadoId, rolAutenticado, ipOrigen);
        return new RespuestaAccion("Listo, agendamos tu cita para el %s a las %s.".formatted(creada.getFecha(), creada.getHora()), EXITO);
    }

    private Optional<Cita> proximaCitaModificable(Paciente paciente) {
        var pagina = consultarCitaUseCase.misCitas(paciente.getUsuarioId(), null, CriterioPaginacion.de(0, 20));
        return pagina.contenido().stream()
                .filter(c -> c.getEstado() == EstadoCita.PROGRAMADA || c.getEstado() == EstadoCita.CONFIRMADA)
                .min(Comparator.comparing(Cita::fechaHora));
    }

    private Optional<LocalDate> parsearFecha(String valor) {
        if (valor == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(LocalDate.parse(valor));
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }

    private Optional<LocalTime> parsearHora(String valor) {
        if (valor == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(LocalTime.parse(valor));
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }

    private String etiqueta(EstadoCita estado) {
        return switch (estado) {
            case PROGRAMADA -> "programada";
            case CONFIRMADA -> "confirmada";
            case ATENDIDA -> "atendida";
            case CANCELADA -> "cancelada";
            case NO_ASISTIO -> "no asistida";
        };
    }
}

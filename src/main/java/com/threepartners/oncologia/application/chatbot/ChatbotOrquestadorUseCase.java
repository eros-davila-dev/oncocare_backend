package com.threepartners.oncologia.application.chatbot;

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
import com.threepartners.oncologia.domain.chatbot.InterpretacionChatbot;
import com.threepartners.oncologia.domain.cita.Cita;
import com.threepartners.oncologia.domain.cita.EstadoCita;
import com.threepartners.oncologia.domain.cita.OrigenCita;
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

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Punto unico de orquestacion del chatbot (seccion 13): Gemini solo
 * interpreta el mensaje (GeminiPort); esta clase es quien decide, con datos
 * reales de la base de datos y las reglas de negocio ya existentes (los mismos
 * casos de uso que usa el resto del sistema), que accion ejecutar. Gemini
 * jamas toca la base de datos ni ejecuta nada por si mismo.
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

    /**
     * Deliberadamente sin @Transactional: la llamada a Gemini es una peticion
     * HTTP externa potencialmente lenta y no debe mantener abierta una
     * conexion de base de datos mientras espera. Los casos de uso que si
     * escriben en la base (agendar/cancelar/reprogramar/confirmar) ya tienen
     * su propia demarcacion transaccional.
     */
    public String procesar(String sesionId, String mensajeUsuario, Long usuarioAutenticadoId, Rol rolAutenticado, String ipOrigen) {
        List<ConversacionChatbot> historial = conversacionChatbotRepositoryPort.listarPorSesion(sesionId, HISTORIAL_MAXIMO);

        Paciente paciente = resolverPaciente(usuarioAutenticadoId, rolAutenticado);
        ContextoUsuarioChatbot contexto = construirContexto(usuarioAutenticadoId, rolAutenticado, paciente);

        InterpretacionChatbot interpretacion = geminiPort.interpretar(mensajeUsuario, historial, contexto);

        String respuestaFinal = ejecutarAccion(interpretacion, paciente, usuarioAutenticadoId, rolAutenticado, ipOrigen);

        conversacionChatbotRepositoryPort.guardar(ConversacionChatbot.builder()
                .pacienteId(paciente != null ? paciente.getId() : null)
                .sesionId(sesionId)
                .mensajeUsuario(mensajeUsuario)
                .respuestaBot(respuestaFinal)
                .intencionDetectada(interpretacion.intencion().name())
                .canal("WEB")
                .fecha(Instant.now())
                .build());

        return respuestaFinal;
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

    private String ejecutarAccion(InterpretacionChatbot interpretacion, Paciente paciente, Long usuarioAutenticadoId, Rol rolAutenticado, String ipOrigen) {
        return switch (interpretacion.intencion()) {
            case GENERAL_QUERY, HELP, ESCALATE_TO_STAFF -> interpretacion.respuestaSugerida();
            case REGISTER_PATIENT -> registroProperties.pacientesHabilitado()
                    ? "Puedes crear tu cuenta de paciente en " + frontendProperties.baseUrl()
                            + "/auth/registro. Solo toma un par de minutos y luego podras agendar tus citas desde aqui."
                    : MENSAJE_REGISTRO_NO_DISPONIBLE;
            case CHECK_APPOINTMENT -> requierePaciente(paciente, usuarioAutenticadoId, rolAutenticado, () -> consultarCitas(paciente));
            case CONFIRM_APPOINTMENT -> requierePaciente(paciente, usuarioAutenticadoId, rolAutenticado, () -> confirmarProximaCita(paciente, usuarioAutenticadoId, rolAutenticado));
            case CANCEL_APPOINTMENT -> requierePaciente(paciente, usuarioAutenticadoId, rolAutenticado, () -> cancelarProximaCita(interpretacion, paciente, usuarioAutenticadoId, rolAutenticado, ipOrigen));
            case RESCHEDULE_APPOINTMENT -> requierePaciente(paciente, usuarioAutenticadoId, rolAutenticado, () -> reprogramarProximaCita(interpretacion, paciente, usuarioAutenticadoId, rolAutenticado, ipOrigen));
            case BOOK_APPOINTMENT -> requierePaciente(paciente, usuarioAutenticadoId, rolAutenticado, () -> agendarCita(interpretacion, paciente, usuarioAutenticadoId, rolAutenticado, ipOrigen));
        };
    }

    /**
     * Las acciones sobre citas exigen un paciente resuelto (cuenta PACIENTE
     * con ficha ya completada); distingue el mensaje segun en cual de los dos
     * pasos del autoservicio (seccion 7) se quedo el usuario.
     */
    private String requierePaciente(Paciente paciente, Long usuarioAutenticadoId, Rol rolAutenticado, java.util.function.Supplier<String> accion) {
        if (paciente == null) {
            boolean autenticadoComoPaciente = usuarioAutenticadoId != null && rolAutenticado == Rol.PACIENTE;
            return autenticadoComoPaciente ? MENSAJE_REQUIERE_PERFIL : MENSAJE_REQUIERE_SESION;
        }
        try {
            return accion.get();
        } catch (DomainException e) {
            return "No pude completar la accion: " + e.getMessage();
        }
    }

    private String consultarCitas(Paciente paciente) {
        var pagina = consultarCitaUseCase.misCitas(paciente.getUsuarioId(), null, CriterioPaginacion.de(0, 5));
        List<Cita> citas = pagina.contenido().stream()
                .filter(c -> c.getEstado() == EstadoCita.PROGRAMADA || c.getEstado() == EstadoCita.CONFIRMADA)
                .sorted(Comparator.comparing(Cita::fechaHora))
                .toList();

        if (citas.isEmpty()) {
            return "No tienes citas proximas programadas. ¿Quieres que te ayude a agendar una?";
        }

        Cita proxima = citas.get(0);
        String base = "Tu proxima cita es el %s a las %s (%s), estado: %s.".formatted(
                proxima.getFecha(), proxima.getHora(), proxima.getTipoConsulta(), etiqueta(proxima.getEstado()));

        return citas.size() > 1 ? base + " Tienes %d citas mas programadas.".formatted(citas.size() - 1) : base;
    }

    private String confirmarProximaCita(Paciente paciente, Long usuarioAutenticadoId, Rol rolAutenticado) {
        Cita cita = proximaCitaModificable(paciente)
                .orElse(null);
        if (cita == null) {
            return "No tienes ninguna cita pendiente de confirmar.";
        }
        if (cita.getEstado() == EstadoCita.CONFIRMADA) {
            return "Tu cita del %s a las %s ya estaba confirmada.".formatted(cita.getFecha(), cita.getHora());
        }
        registrarAsistenciaCitaUseCase.confirmar(cita.getId(), usuarioAutenticadoId, rolAutenticado);
        return "Listo, confirmamos tu cita del %s a las %s.".formatted(cita.getFecha(), cita.getHora());
    }

    private String cancelarProximaCita(InterpretacionChatbot interpretacion, Paciente paciente, Long usuarioAutenticadoId, Rol rolAutenticado, String ipOrigen) {
        Cita cita = proximaCitaModificable(paciente).orElse(null);
        if (cita == null) {
            return "No tienes ninguna cita proxima para cancelar.";
        }
        String motivo = interpretacion.entidad("motivo");
        if (!interpretacion.listoParaEjecutar() || motivo == null) {
            return "Antes de cancelar tu cita del %s a las %s, cuentame brevemente el motivo.".formatted(cita.getFecha(), cita.getHora());
        }
        cancelarCitaUseCase.ejecutar(cita.getId(), motivo, usuarioAutenticadoId, rolAutenticado, ipOrigen);
        return "Tu cita del %s a las %s fue cancelada.".formatted(cita.getFecha(), cita.getHora());
    }

    private String reprogramarProximaCita(InterpretacionChatbot interpretacion, Paciente paciente, Long usuarioAutenticadoId, Rol rolAutenticado, String ipOrigen) {
        Cita cita = proximaCitaModificable(paciente).orElse(null);
        if (cita == null) {
            return "No tienes ninguna cita proxima para reprogramar.";
        }

        Optional<LocalDate> fecha = parsearFecha(interpretacion.entidad("fecha"));
        Optional<LocalTime> hora = parsearHora(interpretacion.entidad("hora"));

        if (!interpretacion.listoParaEjecutar() || fecha.isEmpty() || hora.isEmpty()) {
            return "¿Para que nueva fecha y hora quieres reprogramar tu cita del %s?".formatted(cita.getFecha());
        }

        Cita actualizada = reprogramarCitaUseCase.ejecutar(cita.getId(), fecha.get(), hora.get(), usuarioAutenticadoId, rolAutenticado, ipOrigen);
        return "Tu cita quedo reprogramada para el %s a las %s.".formatted(actualizada.getFecha(), actualizada.getHora());
    }

    private String agendarCita(InterpretacionChatbot interpretacion, Paciente paciente, Long usuarioAutenticadoId, Rol rolAutenticado, String ipOrigen) {
        String especialidadTexto = interpretacion.entidad("especialidad");
        Optional<LocalDate> fecha = parsearFecha(interpretacion.entidad("fecha"));
        Optional<LocalTime> hora = parsearHora(interpretacion.entidad("hora"));

        if (!interpretacion.listoParaEjecutar() || especialidadTexto == null || fecha.isEmpty() || hora.isEmpty()) {
            return interpretacion.respuestaSugerida();
        }

        Especialidad especialidad;
        try {
            especialidad = Especialidad.valueOf(especialidadTexto);
        } catch (IllegalArgumentException e) {
            return "No reconozco esa especialidad. Las disponibles son oncologia clinica, oncologia quirurgica, radioterapia y cuidados paliativos.";
        }

        Usuario medico = usuarioRepositoryPort.listarPorEspecialidad(especialidad).stream().findFirst().orElse(null);
        if (medico == null) {
            return "En este momento no tenemos un medico disponible en esa especialidad. Por favor contacta a recepcion.";
        }

        Cita nuevaCita = Cita.builder()
                .pacienteId(paciente.getId())
                .medicoId(medico.getId())
                .fecha(fecha.get())
                .hora(hora.get())
                .tipoConsulta(interpretacion.entidad("motivo") != null ? interpretacion.entidad("motivo") : "Consulta oncologica")
                .build();

        nuevaCita.setOrigen(OrigenCita.CHATBOT_WEB);
        Cita creada = agendarCitaUseCase.ejecutar(nuevaCita, null, usuarioAutenticadoId, rolAutenticado, ipOrigen);
        return "Listo, agendamos tu cita para el %s a las %s.".formatted(creada.getFecha(), creada.getHora());
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

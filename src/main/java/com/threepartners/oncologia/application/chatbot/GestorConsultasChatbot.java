package com.threepartners.oncologia.application.chatbot;

import com.threepartners.oncologia.domain.chatbot.Intencion;
import com.threepartners.oncologia.domain.chatbot.ResultadoAccion;
import com.threepartners.oncologia.domain.estudio.CanalConsulta;
import com.threepartners.oncologia.domain.estudio.Consulta;
import com.threepartners.oncologia.domain.estudio.ConsultaRepositoryPort;
import com.threepartners.oncologia.domain.estudio.ResultadoConsulta;
import com.threepartners.oncologia.domain.notificacion.NotificadorExternoPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Traduce la conversacion del chatbot en consultas (indicador NCA). Las
 * reglas son deliberadamente conservadoras: ante la duda, la consulta NO se
 * cuenta como resuelta por el bot.
 *
 * - Un turno continua la consulta abierta de la sesion si es la misma
 *   necesidad (misma intencion) o una pregunta informativa intercalada.
 * - Una necesidad distinta abre una consulta nueva y la anterior, si seguia
 *   abierta, se cierra como NO_RESUELTA (el usuario la abandono).
 * - Repetir la misma pregunta informativa poco despues de la respuesta del bot
 *   reabre esa consulta: la respuesta no lo resolvio.
 * - Si tras {@link #MAXIMO_TURNOS} turnos la necesidad sigue sin resolverse, o
 *   la accion pide una persona, la consulta se escala al personal.
 */
@Service
@RequiredArgsConstructor
public class GestorConsultasChatbot {

    static final int MAXIMO_TURNOS = 4;
    static final Duration VENTANA_REFORMULACION = Duration.ofMinutes(10);
    private static final Set<Intencion> INFORMATIVAS = EnumSet.of(Intencion.GENERAL_QUERY, Intencion.HELP);
    private static final int LONGITUD_RESUMEN = 300;

    private final ConsultaRepositoryPort consultaRepositoryPort;
    private final NotificadorExternoPort notificadorExternoPort;
    private final Clock clock;

    public Consulta registrarTurno(String sesionId, CanalConsulta canal, Long pacienteId, Intencion intencion,
                                   ResultadoAccion resultado, String mensaje, Instant inicioTurno) {
        Instant ahora = clock.instant();
        Optional<Consulta> ultima = consultaRepositoryPort.buscarUltimaPorSesion(sesionId);

        Consulta consulta;
        if (ultima.isPresent() && continuaLaNecesidad(ultima.get(), intencion)) {
            consulta = ultima.get();
            consulta.registrarTurno(ahora);
            if (consulta.getPacienteId() == null) {
                consulta.setPacienteId(pacienteId);
            }
            if (INFORMATIVAS.contains(intencion) && !INFORMATIVAS.contains(intencionDe(consulta))) {
                // Pregunta intercalada en medio de un tramite: no cambia el desenlace del tramite.
                return escalarSiExcedeTurnos(consultaRepositoryPort.guardar(consulta), ahora);
            }
        } else if (ultima.isPresent() && esReformulacion(ultima.get(), intencion, ahora)) {
            consulta = ultima.get();
            consulta.reabrirPorReformulacion(ahora);
        } else {
            ultima.filter(Consulta::enManosDelBot).ifPresent(abandonada -> {
                abandonada.cerrar(ResultadoConsulta.NO_RESUELTA, null, ahora);
                consultaRepositoryPort.guardar(abandonada);
            });
            consulta = Consulta.abrir(canal, sesionId, pacienteId, intencion.name(), resumen(mensaje), inicioTurno, ahora);
        }

        aplicar(consulta, resultado, ahora);
        Consulta guardada = consultaRepositoryPort.guardar(consulta);
        if (resultado != ResultadoAccion.ESCALAR) {
            guardada = escalarSiExcedeTurnos(guardada, ahora);
        }
        if (guardada.getResultado() == ResultadoConsulta.ESCALADA) {
            avisarAlPersonal(guardada);
        }
        return guardada;
    }

    /** "Hablar con una persona": escala la necesidad en curso o abre una nueva ya escalada. */
    public Consulta escalarPorPedidoDelUsuario(String sesionId, CanalConsulta canal, Long pacienteId) {
        Instant ahora = clock.instant();
        Consulta consulta = consultaRepositoryPort.buscarUltimaPorSesion(sesionId)
                .filter(c -> c.enManosDelBot() || c.getResultado() == ResultadoConsulta.RESUELTA_BOT)
                .orElseGet(() -> Consulta.abrir(canal, sesionId, pacienteId, Intencion.ESCALATE_TO_STAFF.name(),
                        "El usuario pidio hablar con una persona", ahora, ahora));
        consulta.escalar(ahora);
        Consulta guardada = consultaRepositoryPort.guardar(consulta);
        avisarAlPersonal(guardada);
        return guardada;
    }

    private boolean continuaLaNecesidad(Consulta ultima, Intencion intencion) {
        if (!ultima.enManosDelBot()) {
            return false;
        }
        return intencion.name().equals(ultima.getIntencion()) || INFORMATIVAS.contains(intencion);
    }

    private boolean esReformulacion(Consulta ultima, Intencion intencion, Instant ahora) {
        return ultima.getResultado() == ResultadoConsulta.RESUELTA_BOT
                && INFORMATIVAS.contains(intencion)
                && intencion.name().equals(ultima.getIntencion())
                && ultima.getCerradaEn() != null
                && Duration.between(ultima.getCerradaEn(), ahora).compareTo(VENTANA_REFORMULACION) <= 0;
    }

    private void aplicar(Consulta consulta, ResultadoAccion resultado, Instant ahora) {
        switch (resultado) {
            case EXITO, INFORMATIVA -> consulta.cerrar(ResultadoConsulta.RESUELTA_BOT, null, ahora);
            case ESCALAR -> consulta.escalar(ahora);
            case REQUIERE_DATOS, REQUIERE_SESION, ERROR_NEGOCIO -> {
                // Sigue abierta: el usuario puede completar los datos en el siguiente turno.
            }
        }
    }

    private Consulta escalarSiExcedeTurnos(Consulta consulta, Instant ahora) {
        if (consulta.enManosDelBot() && consulta.getTurnos() >= MAXIMO_TURNOS) {
            consulta.escalar(ahora);
            return consultaRepositoryPort.guardar(consulta);
        }
        return consulta;
    }

    /**
     * Solo el id y el canal: el aviso viaja por Telegram al grupo del personal
     * y el texto del paciente puede contener datos clinicos (Ley 29733). El
     * detalle se lee en la bandeja de la intranet, con sesion iniciada.
     */
    private void avisarAlPersonal(Consulta consulta) {
        Map<String, Object> aviso = new HashMap<>();
        aviso.put("consultaId", consulta.getId());
        aviso.put("canal", consulta.getCanal().name());
        notificadorExternoPort.dispararWorkflow("/webhook/consultas/escalada", aviso);
    }

    private static Intencion intencionDe(Consulta consulta) {
        try {
            return Intencion.valueOf(consulta.getIntencion());
        } catch (IllegalArgumentException | NullPointerException e) {
            return Intencion.ESCALATE_TO_STAFF;
        }
    }

    private static String resumen(String mensaje) {
        String limpio = mensaje == null ? "" : mensaje.strip();
        return limpio.length() <= LONGITUD_RESUMEN ? limpio : limpio.substring(0, LONGITUD_RESUMEN);
    }
}

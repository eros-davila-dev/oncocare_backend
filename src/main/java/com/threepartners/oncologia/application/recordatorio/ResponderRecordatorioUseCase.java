package com.threepartners.oncologia.application.recordatorio;

import com.threepartners.oncologia.application.chatbot.GestorConsultasChatbot;
import com.threepartners.oncologia.application.cita.AccionesCitaPacienteService;
import com.threepartners.oncologia.config.FrontendProperties;
import com.threepartners.oncologia.domain.chatbot.Intencion;
import com.threepartners.oncologia.domain.cita.Cita;
import com.threepartners.oncologia.domain.cita.EstadoCita;
import com.threepartners.oncologia.domain.estudio.CanalConsulta;
import com.threepartners.oncologia.domain.paciente.Paciente;
import com.threepartners.oncologia.domain.paciente.PacienteRepositoryPort;
import com.threepartners.oncologia.domain.recordatorio.Recordatorio;
import com.threepartners.oncologia.domain.recordatorio.RecordatorioRepositoryPort;
import com.threepartners.oncologia.domain.recordatorio.RespuestaRecordatorio;
import com.threepartners.oncologia.domain.shared.exception.RecursoNoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * Botones del recordatorio en Telegram: Confirmo / Reprogramar / Cancelar.
 * Confirmar y cancelar se ejecutan al instante (cancelar con aviso saca la
 * cita del denominador del TNS: no es una inasistencia). Reprogramar se
 * deriva al personal, que coordina la nueva fecha con el paciente.
 */
@Service
@RequiredArgsConstructor
public class ResponderRecordatorioUseCase {

    private final RecordatorioRepositoryPort recordatorioRepositoryPort;
    private final PacienteRepositoryPort pacienteRepositoryPort;
    private final AccionesCitaPacienteService accionesCita;
    private final GestorConsultasChatbot gestorConsultas;
    private final FrontendProperties frontendProperties;
    private final Clock clock;

    @Transactional
    public String responder(Long chatId, Long recordatorioId, AccionRecordatorio accion) {
        Paciente paciente = pacienteRepositoryPort.buscarPorTelegramChatId(chatId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Paciente vinculado", chatId));
        Recordatorio recordatorio = recordatorioRepositoryPort.buscarPorId(recordatorioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Recordatorio", recordatorioId));
        Cita cita = accionesCita.citaDelPaciente(recordatorio.getCitaId(), paciente);

        if (cita.esFinal()) {
            return "Esta cita ya no esta activa (" + cita.getEstado().name().toLowerCase().replace('_', ' ') + ").";
        }

        String respuesta = switch (accion) {
            case CONFIRMAR -> {
                recordatorio.registrarRespuesta(RespuestaRecordatorio.CONFIRMO, clock.instant());
                if (cita.getEstado() == EstadoCita.CONFIRMADA) {
                    yield "Tu cita ya estaba confirmada. ¡Te esperamos!";
                }
                accionesCita.confirmar(cita, paciente);
                yield "¡Gracias! Tu cita quedo confirmada. Te esperamos.";
            }
            case CANCELAR -> {
                recordatorio.registrarRespuesta(RespuestaRecordatorio.CANCELO, clock.instant());
                accionesCita.cancelar(cita, paciente, "Cancelada por el paciente desde Telegram");
                yield "Listo, cancelamos tu cita. Gracias por avisarnos con tiempo; si necesitas una nueva, escribenos aqui.";
            }
            case REPROGRAMAR -> {
                recordatorio.registrarRespuesta(RespuestaRecordatorio.PIDIO_REPROGRAMAR, clock.instant());
                gestorConsultas.escalarSolicitud("tg-" + chatId, CanalConsulta.TELEGRAM, paciente.getId(),
                        Intencion.RESCHEDULE_APPOINTMENT, "Pide reprogramar su cita del " + cita.getFecha() + " a las " + cita.getHora());
                yield "Entendido. Recepcion te contactara para acordar una nueva fecha. Tambien puedes hacerlo en "
                        + frontendProperties.baseUrl() + "/mis-citas";
            }
        };
        recordatorioRepositoryPort.guardar(recordatorio);
        return respuesta;
    }

    public enum AccionRecordatorio {
        CONFIRMAR,
        CANCELAR,
        REPROGRAMAR
    }
}

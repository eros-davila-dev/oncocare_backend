package com.threepartners.oncologia.application.recordatorio;

import com.threepartners.oncologia.domain.auditoria.event.OperacionAuditadaEvent;
import com.threepartners.oncologia.domain.cita.Cita;
import com.threepartners.oncologia.domain.cita.CitaRepositoryPort;
import com.threepartners.oncologia.domain.paciente.Paciente;
import com.threepartners.oncologia.domain.paciente.PacienteRepositoryPort;
import com.threepartners.oncologia.domain.recordatorio.CanalRecordatorio;
import com.threepartners.oncologia.domain.recordatorio.EstadoRecordatorio;
import com.threepartners.oncologia.domain.recordatorio.Recordatorio;
import com.threepartners.oncologia.domain.recordatorio.RecordatorioRepositoryPort;
import com.threepartners.oncologia.domain.shared.exception.RecursoNoEncontradoException;
import com.threepartners.oncologia.domain.shared.exception.ValidacionDeNegocioException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Pacientes sin Telegram: recepcion los llama el dia anterior a su cita. Asi
 * el recordatorio (y su efecto en el TNS) no depende de que el paciente use
 * un smartphone.
 */
@Service
@RequiredArgsConstructor
public class LlamadasRecordatorioUseCase {

    private final RecordatorioRepositoryPort recordatorioRepositoryPort;
    private final CitaRepositoryPort citaRepositoryPort;
    private final PacienteRepositoryPort pacienteRepositoryPort;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    @Transactional
    public List<LlamadaPendiente> pendientes() {
        Instant ahora = clock.instant();
        List<LlamadaPendiente> llamadas = new ArrayList<>();
        for (Recordatorio recordatorio : recordatorioRepositoryPort.listarPendientesVencidos(CanalRecordatorio.LLAMADA, ahora)) {
            Cita cita = citaRepositoryPort.buscarPorId(recordatorio.getCitaId()).orElse(null);
            Paciente paciente = cita != null ? pacienteRepositoryPort.buscarPorId(cita.getPacienteId()).orElse(null) : null;
            if (cita == null || paciente == null || !recordatorio.vigentePara(cita, ahora)) {
                recordatorio.cancelar();
                recordatorioRepositoryPort.guardar(recordatorio);
                continue;
            }
            llamadas.add(new LlamadaPendiente(recordatorio.getId(), cita.getId(), paciente.nombreCompleto(),
                    paciente.getTelefono(), cita.getFecha().toString(), MensajesRecordatorio.horaTexto(cita),
                    recordatorio.getIntentos()));
        }
        return llamadas;
    }

    /** "No contesto" deja la llamada en la lista hasta agotar los intentos. */
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    @Transactional
    public void registrarLlamada(Long recordatorioId, boolean contesto, Long usuarioId, String ipOrigen) {
        Recordatorio recordatorio = recordatorioRepositoryPort.buscarPorId(recordatorioId)
                .filter(r -> r.getCanal() == CanalRecordatorio.LLAMADA)
                .orElseThrow(() -> new RecursoNoEncontradoException("Recordatorio por llamada", recordatorioId));
        if (recordatorio.getEstado() != EstadoRecordatorio.PENDIENTE) {
            throw new ValidacionDeNegocioException("Esta llamada ya fue registrada");
        }
        recordatorio.tomar(clock.instant());
        if (contesto) {
            recordatorio.marcarEnviado(null, clock.instant());
        } else {
            recordatorio.marcarFallido("No contesto");
        }
        recordatorioRepositoryPort.guardar(recordatorio);
        eventPublisher.publishEvent(OperacionAuditadaEvent.exito(usuarioId, "RECORDATORIO_LLAMADA_REGISTRADA",
                "RECORDATORIO", recordatorioId, null, "contesto=" + contesto, ipOrigen));
    }

    public record LlamadaPendiente(Long recordatorioId, Long citaId, String pacienteNombre, String telefono,
                                   String fecha, String hora, int intentosPrevios) {
    }
}

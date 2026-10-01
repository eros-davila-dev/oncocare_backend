package com.threepartners.oncologia.application.estudio;

import com.threepartners.oncologia.domain.auditoria.event.OperacionAuditadaEvent;
import com.threepartners.oncologia.domain.estudio.MotivoExclusion;
import com.threepartners.oncologia.domain.estudio.ParticipanteEstudio;
import com.threepartners.oncologia.domain.estudio.ParticipanteEstudioRepositoryPort;
import com.threepartners.oncologia.domain.estudio.ParticipanteResumen;
import com.threepartners.oncologia.domain.paciente.PacienteRepositoryPort;
import com.threepartners.oncologia.domain.shared.exception.ConflictoDeNegocioException;
import com.threepartners.oncologia.domain.shared.exception.RecursoNoEncontradoException;
import com.threepartners.oncologia.domain.shared.exception.ValidacionDeNegocioException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/**
 * Muestra del estudio: solo los pacientes con consentimiento informado
 * firmado entran al alcance MUESTRA de los indicadores y a la tabla pareada.
 */
@Service
@RequiredArgsConstructor
public class GestionarParticipantesUseCase {

    private final ParticipanteEstudioRepositoryPort participanteRepositoryPort;
    private final PacienteRepositoryPort pacienteRepositoryPort;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @PreAuthorize("hasAnyRole('ADMIN', 'INVESTIGADOR')")
    @Transactional(readOnly = true)
    public List<ParticipanteResumen> listar() {
        return participanteRepositoryPort.listar();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'INVESTIGADOR')")
    @Transactional
    public ParticipanteEstudio incluir(Long pacienteId, LocalDate fechaConsentimiento, String observacion,
                                       Long usuarioId, String ipOrigen) {
        pacienteRepositoryPort.buscarPorId(pacienteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Paciente", pacienteId));
        if (participanteRepositoryPort.existePorPacienteId(pacienteId)) {
            throw new ConflictoDeNegocioException("El paciente ya forma parte de la muestra del estudio");
        }
        if (fechaConsentimiento == null || fechaConsentimiento.isAfter(LocalDate.now(clock))) {
            throw new ValidacionDeNegocioException("La fecha del consentimiento informado no puede ser futura");
        }

        String codigo = ParticipanteEstudio.codigoParaNumero(participanteRepositoryPort.maximoNumeroCodigo() + 1);
        ParticipanteEstudio guardado = participanteRepositoryPort.guardar(ParticipanteEstudio.builder()
                .pacienteId(pacienteId)
                .codigo(codigo)
                .fechaConsentimiento(fechaConsentimiento)
                .incluido(true)
                .observacion(observacion)
                .fechaInclusion(clock.instant())
                .build());

        eventPublisher.publishEvent(OperacionAuditadaEvent.exito(usuarioId, "ESTUDIO_PARTICIPANTE_INCLUIDO",
                "PARTICIPANTE_ESTUDIO", guardado.getId(), null,
                "codigo=%s;paciente=%d".formatted(codigo, pacienteId), ipOrigen));
        return guardado;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'INVESTIGADOR')")
    @Transactional
    public ParticipanteEstudio excluir(Long participanteId, MotivoExclusion motivo, String observacion,
                                       Long usuarioId, String ipOrigen) {
        ParticipanteEstudio participante = obtener(participanteId);
        participante.excluir(motivo, observacion, clock.instant());
        ParticipanteEstudio guardado = participanteRepositoryPort.guardar(participante);
        eventPublisher.publishEvent(OperacionAuditadaEvent.exito(usuarioId, "ESTUDIO_PARTICIPANTE_EXCLUIDO",
                "PARTICIPANTE_ESTUDIO", participanteId, "incluido=true", "motivo=" + motivo, ipOrigen));
        return guardado;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'INVESTIGADOR')")
    @Transactional
    public ParticipanteEstudio reincorporar(Long participanteId, Long usuarioId, String ipOrigen) {
        ParticipanteEstudio participante = obtener(participanteId);
        String previo = "motivo=" + participante.getMotivoExclusion();
        participante.reincorporar();
        ParticipanteEstudio guardado = participanteRepositoryPort.guardar(participante);
        eventPublisher.publishEvent(OperacionAuditadaEvent.exito(usuarioId, "ESTUDIO_PARTICIPANTE_REINCORPORADO",
                "PARTICIPANTE_ESTUDIO", participanteId, previo, "incluido=true", ipOrigen));
        return guardado;
    }

    private ParticipanteEstudio obtener(Long participanteId) {
        return participanteRepositoryPort.buscarPorId(participanteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Participante del estudio", participanteId));
    }
}

package com.threepartners.oncologia.application.cita;

import com.threepartners.oncologia.domain.auditoria.ResultadoAuditoria;
import com.threepartners.oncologia.domain.auditoria.event.CitaCanceladaEvent;
import com.threepartners.oncologia.domain.cita.Cita;
import com.threepartners.oncologia.domain.cita.CitaRepositoryPort;
import com.threepartners.oncologia.domain.paciente.Paciente;
import com.threepartners.oncologia.domain.paciente.PacienteRepositoryPort;
import com.threepartners.oncologia.domain.shared.exception.RecursoNoEncontradoException;
import com.threepartners.oncologia.domain.usuario.Rol;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CancelarCitaUseCase {

    private final CitaRepositoryPort citaRepositoryPort;
    private final PacienteRepositoryPort pacienteRepositoryPort;
    private final ApplicationEventPublisher eventPublisher;

    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA', 'MEDICO', 'PACIENTE')")
    @Transactional
    public Cita ejecutar(Long citaId, String motivo, Long usuarioEjecutorId, Rol rolEjecutor, String ipOrigen) {
        Cita cita = citaRepositoryPort.buscarPorId(citaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cita", citaId));

        verificarPropiedadSiEsPaciente(cita, usuarioEjecutorId, rolEjecutor);

        String valoresPrevios = "estado=%s".formatted(cita.getEstado());
        cita.cancelar();
        cita.setObservaciones(motivo);
        Cita cancelada = citaRepositoryPort.guardar(cita);

        eventPublisher.publishEvent(new CitaCanceladaEvent(
                usuarioEjecutorId,
                String.valueOf(citaId),
                valoresPrevios,
                ipOrigen,
                ResultadoAuditoria.EXITO));

        return cancelada;
    }

    private void verificarPropiedadSiEsPaciente(Cita cita, Long usuarioEjecutorId, Rol rolEjecutor) {
        if (rolEjecutor != Rol.PACIENTE) {
            return;
        }
        Paciente paciente = pacienteRepositoryPort.buscarPorUsuarioId(usuarioEjecutorId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cita", cita.getId()));
        if (!paciente.getId().equals(cita.getPacienteId())) {
            throw new RecursoNoEncontradoException("Cita", cita.getId());
        }
    }
}

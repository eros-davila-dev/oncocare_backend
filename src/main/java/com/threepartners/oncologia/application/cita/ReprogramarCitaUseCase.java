package com.threepartners.oncologia.application.cita;

import com.threepartners.oncologia.domain.auditoria.ResultadoAuditoria;
import com.threepartners.oncologia.domain.auditoria.event.CitaReprogramadaEvent;
import com.threepartners.oncologia.domain.cita.Cita;
import com.threepartners.oncologia.domain.cita.CitaRepositoryPort;
import com.threepartners.oncologia.domain.cita.EstadoCita;
import com.threepartners.oncologia.domain.paciente.Paciente;
import com.threepartners.oncologia.domain.paciente.PacienteRepositoryPort;
import com.threepartners.oncologia.domain.shared.exception.ConflictoDeNegocioException;
import com.threepartners.oncologia.domain.shared.exception.RecursoNoEncontradoException;
import com.threepartners.oncologia.domain.usuario.Rol;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
public class ReprogramarCitaUseCase {

    private final CitaRepositoryPort citaRepositoryPort;
    private final PacienteRepositoryPort pacienteRepositoryPort;
    private final ApplicationEventPublisher eventPublisher;

    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA', 'MEDICO', 'PACIENTE')")
    @Transactional
    public Cita ejecutar(Long citaId, LocalDate nuevaFecha, LocalTime nuevaHora, Long usuarioEjecutorId, Rol rolEjecutor, String ipOrigen) {
        Cita cita = citaRepositoryPort.buscarPorId(citaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cita", citaId));

        verificarPropiedadSiEsPaciente(cita, usuarioEjecutorId, rolEjecutor);

        if (citaRepositoryPort.existeSolapamiento(cita.getMedicoId(), nuevaFecha, nuevaHora, citaId)) {
            throw new ConflictoDeNegocioException("El medico ya tiene una cita programada en ese horario");
        }

        String valoresPrevios = "fecha=%s;hora=%s".formatted(cita.getFecha(), cita.getHora());

        cita.setFecha(nuevaFecha);
        cita.setHora(nuevaHora);
        cita.setEstado(EstadoCita.PROGRAMADA);
        Cita actualizada = citaRepositoryPort.guardar(cita);

        eventPublisher.publishEvent(new CitaReprogramadaEvent(
                usuarioEjecutorId,
                String.valueOf(citaId),
                valoresPrevios,
                "fecha=%s;hora=%s".formatted(nuevaFecha, nuevaHora),
                ipOrigen,
                ResultadoAuditoria.EXITO));

        return actualizada;
    }

    /**
     * Seccion 31: un paciente solo puede operar sobre sus propias citas.
     * Ante cualquier discrepancia se responde 404 (no 403) para no confirmar
     * la existencia de una cita ajena.
     */
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

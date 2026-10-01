package com.threepartners.oncologia.application.tratamiento;

import com.threepartners.oncologia.domain.auditoria.ResultadoAuditoria;
import com.threepartners.oncologia.domain.auditoria.event.CicloTratamientoProgramadoEvent;
import com.threepartners.oncologia.domain.paciente.PacienteRepositoryPort;
import com.threepartners.oncologia.domain.shared.exception.RecursoNoEncontradoException;
import com.threepartners.oncologia.domain.tratamiento.CicloTratamiento;
import com.threepartners.oncologia.domain.tratamiento.CicloTratamientoRepositoryPort;
import com.threepartners.oncologia.domain.tratamiento.EstadoCicloTratamiento;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProgramarCicloTratamientoUseCase {

    private final CicloTratamientoRepositoryPort cicloTratamientoRepositoryPort;
    private final PacienteRepositoryPort pacienteRepositoryPort;
    private final ApplicationEventPublisher eventPublisher;

    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO')")
    @Transactional
    public CicloTratamiento ejecutar(CicloTratamiento ciclo, Long usuarioEjecutorId, String ipOrigen) {
        pacienteRepositoryPort.buscarPorId(ciclo.getPacienteId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Paciente", ciclo.getPacienteId()));

        ciclo.setEstado(EstadoCicloTratamiento.PROGRAMADO);
        CicloTratamiento guardado = cicloTratamientoRepositoryPort.guardar(ciclo);

        eventPublisher.publishEvent(new CicloTratamientoProgramadoEvent(
                usuarioEjecutorId,
                String.valueOf(guardado.getId()),
                "paciente=%d;tipo=%s;sesion=%d/%d".formatted(
                        guardado.getPacienteId(), guardado.getTipoTratamiento(),
                        guardado.getNumeroSesion(), guardado.getTotalSesionesEsquema()),
                ipOrigen,
                ResultadoAuditoria.EXITO));

        return guardado;
    }
}

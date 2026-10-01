package com.threepartners.oncologia.application.paciente;

import com.threepartners.oncologia.domain.auditoria.ResultadoAuditoria;
import com.threepartners.oncologia.domain.auditoria.event.PacienteRegistradoEvent;
import com.threepartners.oncologia.domain.paciente.Paciente;
import com.threepartners.oncologia.domain.paciente.PacienteRepositoryPort;
import com.threepartners.oncologia.domain.shared.exception.ConflictoDeNegocioException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class RegistrarPacienteUseCase {

    private final PacienteRepositoryPort pacienteRepositoryPort;
    private final ApplicationEventPublisher eventPublisher;

    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    @Transactional
    public Paciente ejecutar(Paciente paciente, Long usuarioEjecutorId, String ipOrigen) {
        if (pacienteRepositoryPort.existePorDocumento(paciente.getDocumentoIdentidad())) {
            throw new ConflictoDeNegocioException(
                    "Ya existe un paciente registrado con el documento de identidad: " + paciente.getDocumentoIdentidad());
        }

        paciente.setActivo(true);
        paciente.setFechaRegistro(Instant.now());
        Paciente guardado = pacienteRepositoryPort.guardar(paciente);

        eventPublisher.publishEvent(new PacienteRegistradoEvent(
                usuarioEjecutorId,
                String.valueOf(guardado.getId()),
                "documento=%s".formatted(guardado.getDocumentoIdentidad()),
                ipOrigen,
                ResultadoAuditoria.EXITO));

        return guardado;
    }
}

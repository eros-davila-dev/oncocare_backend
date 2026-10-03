package com.threepartners.oncologia.application.paciente;

import com.threepartners.oncologia.application.estudio.CerrarMedicionRegistroService;
import com.threepartners.oncologia.domain.auditoria.ResultadoAuditoria;
import com.threepartners.oncologia.domain.auditoria.event.PacienteRegistradoEvent;
import com.threepartners.oncologia.domain.estudio.TipoMedicion;
import com.threepartners.oncologia.domain.paciente.Paciente;
import com.threepartners.oncologia.domain.paciente.PacienteRepositoryPort;
import com.threepartners.oncologia.domain.shared.exception.ConflictoDeNegocioException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@RequiredArgsConstructor
public class RegistrarPacienteUseCase {

    private final PacienteRepositoryPort pacienteRepositoryPort;
    private final ApplicationEventPublisher eventPublisher;
    private final CerrarMedicionRegistroService cerrarMedicionRegistroService;
    private final Clock clock;

    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    @Transactional
    public Paciente ejecutar(Paciente paciente, Long medicionId, Long usuarioEjecutorId, String ipOrigen) {
        paciente.validarDatosDeContacto();
        if (pacienteRepositoryPort.existePorDocumento(paciente.getDocumentoIdentidad())) {
            throw new ConflictoDeNegocioException(
                    "Ya existe un paciente registrado con el documento de identidad: " + paciente.getDocumentoIdentidad());
        }

        paciente.setActivo(true);
        paciente.setFechaRegistro(clock.instant());
        Paciente guardado = pacienteRepositoryPort.guardar(paciente);
        cerrarMedicionRegistroService.cerrar(medicionId, TipoMedicion.REGISTRO_PACIENTE, usuarioEjecutorId,
                guardado.getId(), guardado.getId());

        eventPublisher.publishEvent(new PacienteRegistradoEvent(
                usuarioEjecutorId,
                String.valueOf(guardado.getId()),
                "documento=%s".formatted(guardado.getDocumentoIdentidad()),
                ipOrigen,
                ResultadoAuditoria.EXITO));

        return guardado;
    }
}

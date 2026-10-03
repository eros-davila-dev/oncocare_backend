package com.threepartners.oncologia.application.paciente;

import com.threepartners.oncologia.application.estudio.CerrarMedicionRegistroService;
import com.threepartners.oncologia.domain.auditoria.ResultadoAuditoria;
import com.threepartners.oncologia.domain.auditoria.event.PacienteActualizadoEvent;
import com.threepartners.oncologia.domain.paciente.ConvenioSeguro;
import com.threepartners.oncologia.domain.estudio.TipoMedicion;
import com.threepartners.oncologia.domain.paciente.Paciente;
import com.threepartners.oncologia.domain.paciente.PacienteRepositoryPort;
import com.threepartners.oncologia.domain.shared.exception.ConflictoDeNegocioException;
import com.threepartners.oncologia.domain.shared.exception.RecursoNoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class ActualizarPacienteUseCase {

    private final PacienteRepositoryPort pacienteRepositoryPort;
    private final ApplicationEventPublisher eventPublisher;
    private final CerrarMedicionRegistroService cerrarMedicionRegistroService;

    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA', 'MEDICO')")
    @Transactional
    public Paciente ejecutar(Long id, Paciente datos, Long medicionId, Long usuarioEjecutorId, String ipOrigen) {
        Paciente existente = pacienteRepositoryPort.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Paciente", id));

        if (!existente.getDocumentoIdentidad().equals(datos.getDocumentoIdentidad())
                && pacienteRepositoryPort.existePorDocumentoYNoId(datos.getDocumentoIdentidad(), id)) {
            throw new ConflictoDeNegocioException(
                    "Ya existe otro paciente registrado con el documento de identidad: " + datos.getDocumentoIdentidad());
        }

        String valoresPrevios = resumen(existente);

        existente.setNombres(datos.getNombres());
        existente.setApellidos(datos.getApellidos());
        existente.setDocumentoIdentidad(datos.getDocumentoIdentidad());
        existente.setFechaNacimiento(datos.getFechaNacimiento());
        existente.setTelefono(datos.getTelefono());
        existente.setEmail(datos.getEmail());
        existente.setDireccion(datos.getDireccion());
        existente.setTipoCancer(datos.getTipoCancer());
        existente.setEstadioClinico(datos.getEstadioClinico());
        existente.setFechaDiagnostico(datos.getFechaDiagnostico());
        existente.setMedicoTratanteId(datos.getMedicoTratanteId());
        existente.setConvenioSeguro(datos.getConvenioSeguro());
        existente.setContactoEmergenciaNombre(datos.getContactoEmergenciaNombre());
        existente.setContactoEmergenciaTelefono(datos.getContactoEmergenciaTelefono());
        existente.setContactoEmergenciaEmail(datos.getContactoEmergenciaEmail());
        existente.setContactoRecibeRecordatorios(datos.isContactoRecibeRecordatorios());
        existente.validarDatosDeContacto();

        Paciente actualizado = pacienteRepositoryPort.guardar(existente);
        cerrarMedicionRegistroService.cerrar(medicionId, TipoMedicion.ACTUALIZACION_PACIENTE, usuarioEjecutorId,
                actualizado.getId(), actualizado.getId());

        eventPublisher.publishEvent(new PacienteActualizadoEvent(
                usuarioEjecutorId,
                String.valueOf(id),
                valoresPrevios,
                resumen(actualizado),
                ipOrigen,
                ResultadoAuditoria.EXITO));

        return actualizado;
    }

    private String resumen(Paciente paciente) {
        ConvenioSeguro convenio = paciente.getConvenioSeguro();
        LocalDate diagnostico = paciente.getFechaDiagnostico();
        return "documento=%s;convenio=%s;fechaDiagnostico=%s".formatted(
                paciente.getDocumentoIdentidad(),
                convenio != null ? convenio.name() : "N/A",
                diagnostico != null ? diagnostico.toString() : "N/A");
    }
}

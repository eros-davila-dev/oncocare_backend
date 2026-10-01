package com.threepartners.oncologia.application.cita;

import com.threepartners.oncologia.application.estudio.CerrarMedicionRegistroService;
import com.threepartners.oncologia.domain.auditoria.ResultadoAuditoria;
import com.threepartners.oncologia.domain.auditoria.event.CitaAgendadaEvent;
import com.threepartners.oncologia.domain.cita.Cita;
import com.threepartners.oncologia.domain.cita.CitaRepositoryPort;
import com.threepartners.oncologia.domain.cita.EstadoCita;
import com.threepartners.oncologia.domain.cita.OrigenCita;
import com.threepartners.oncologia.domain.estudio.TipoMedicion;
import com.threepartners.oncologia.domain.paciente.Paciente;
import com.threepartners.oncologia.domain.paciente.PacienteRepositoryPort;
import com.threepartners.oncologia.domain.shared.exception.ConflictoDeNegocioException;
import com.threepartners.oncologia.domain.shared.exception.RecursoNoEncontradoException;
import com.threepartners.oncologia.domain.shared.exception.ValidacionDeNegocioException;
import com.threepartners.oncologia.domain.usuario.Rol;
import com.threepartners.oncologia.domain.usuario.Usuario;
import com.threepartners.oncologia.domain.usuario.UsuarioRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@RequiredArgsConstructor
public class AgendarCitaUseCase {

    private final CitaRepositoryPort citaRepositoryPort;
    private final PacienteRepositoryPort pacienteRepositoryPort;
    private final UsuarioRepositoryPort usuarioRepositoryPort;
    private final ApplicationEventPublisher eventPublisher;
    private final CerrarMedicionRegistroService cerrarMedicionRegistroService;
    private final Clock clock;

    /**
     * @param medicionId sesion de medicion del TPR abierta al mostrar el
     *                   formulario (null si la cita no viene de un formulario,
     *                   p. ej. el chatbot); se cierra en esta misma transaccion.
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA', 'MEDICO', 'PACIENTE')")
    @Transactional
    public Cita ejecutar(Cita cita, Long medicionId, Long usuarioEjecutorId, Rol rolEjecutor, String ipOrigen) {
        Paciente paciente = pacienteRepositoryPort.buscarPorId(cita.getPacienteId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Paciente", cita.getPacienteId()));

        if (rolEjecutor == Rol.PACIENTE && !usuarioEjecutorId.equals(paciente.getUsuarioId())) {
            // Seccion 31: un paciente solo puede agendar para si mismo. 404 en
            // vez de 403 para no confirmar la existencia del id ajeno.
            throw new RecursoNoEncontradoException("Paciente", cita.getPacienteId());
        }

        Usuario medico = usuarioRepositoryPort.buscarPorId(cita.getMedicoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Medico", cita.getMedicoId()));

        if (medico.getRol() != Rol.MEDICO || medico.getEspecialidad() == null) {
            throw new ValidacionDeNegocioException("El usuario seleccionado no es un medico con especialidad valida");
        }

        if (citaRepositoryPort.existeSolapamiento(cita.getMedicoId(), cita.getFecha(), cita.getHora(), null)) {
            throw new ConflictoDeNegocioException("El medico ya tiene una cita programada en ese horario");
        }

        cita.setEstado(EstadoCita.PROGRAMADA);
        cita.setFechaCreacion(clock.instant());
        if (cita.getOrigen() == null) {
            cita.setOrigen(rolEjecutor == Rol.PACIENTE ? OrigenCita.PORTAL : OrigenCita.INTRANET);
        }
        Cita guardada = citaRepositoryPort.guardar(cita);
        cerrarMedicionRegistroService.cerrar(medicionId, TipoMedicion.REGISTRO_CITA, usuarioEjecutorId,
                guardada.getPacienteId(), guardada.getId());

        eventPublisher.publishEvent(new CitaAgendadaEvent(
                usuarioEjecutorId,
                String.valueOf(guardada.getId()),
                "paciente=%d;medico=%d;fecha=%s;hora=%s".formatted(
                        guardada.getPacienteId(), guardada.getMedicoId(), guardada.getFecha(), guardada.getHora()),
                ipOrigen,
                ResultadoAuditoria.EXITO));

        return guardada;
    }
}

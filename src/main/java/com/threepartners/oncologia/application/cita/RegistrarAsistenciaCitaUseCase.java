package com.threepartners.oncologia.application.cita;

import com.threepartners.oncologia.domain.auditoria.event.OperacionAuditadaEvent;
import com.threepartners.oncologia.domain.cita.Cita;
import com.threepartners.oncologia.domain.cita.CitaRepositoryPort;
import com.threepartners.oncologia.domain.cita.EstadoCita;
import com.threepartners.oncologia.domain.estudio.CorreccionMedicion;
import com.threepartners.oncologia.domain.estudio.CorreccionMedicion.EntidadCorregida;
import com.threepartners.oncologia.domain.estudio.CorreccionMedicionRepositoryPort;
import com.threepartners.oncologia.domain.paciente.Paciente;
import com.threepartners.oncologia.domain.paciente.PacienteRepositoryPort;
import com.threepartners.oncologia.domain.shared.exception.RecursoNoEncontradoException;
import com.threepartners.oncologia.domain.usuario.Rol;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;

/**
 * Desenlace de la cita: es el dato del indicador TNS (ausentismo). Se guarda
 * quien lo registro y cuando, y queda en la auditoria.
 */
@Service
@RequiredArgsConstructor
public class RegistrarAsistenciaCitaUseCase {

    private final CitaRepositoryPort citaRepositoryPort;
    private final PacienteRepositoryPort pacienteRepositoryPort;
    private final ApplicationEventPublisher eventPublisher;
    private final CorreccionMedicionRepositoryPort correccionMedicionRepositoryPort;
    private final Clock clock;

    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA', 'MEDICO')")
    @Transactional
    public Cita marcarAtendida(Long citaId, Long usuarioEjecutorId, String ipOrigen) {
        Cita cita = obtener(citaId);
        String previo = "estado=" + cita.getEstado();
        cita.exigirFechaAlcanzada(LocalDate.now(clock));
        cita.atender(usuarioEjecutorId, clock.instant());
        Cita guardada = citaRepositoryPort.guardar(cita);
        eventPublisher.publishEvent(OperacionAuditadaEvent.exito(usuarioEjecutorId, "CITA_ATENDIDA", "CITA",
                citaId, previo, "estado=ATENDIDA", ipOrigen));
        return guardada;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA', 'MEDICO')")
    @Transactional
    public Cita marcarNoAsistio(Long citaId, Long usuarioEjecutorId, String ipOrigen) {
        Cita cita = obtener(citaId);
        String previo = "estado=" + cita.getEstado();
        cita.exigirFechaAlcanzada(LocalDate.now(clock));
        cita.marcarNoAsistio(usuarioEjecutorId, clock.instant(), false);
        Cita guardada = citaRepositoryPort.guardar(cita);
        eventPublisher.publishEvent(OperacionAuditadaEvent.exito(usuarioEjecutorId, "CITA_NO_ASISTIO", "CITA",
                citaId, previo, "estado=NO_ASISTIO", ipOrigen));
        return guardada;
    }

    /**
     * Corrige un desenlace ya registrado (incluido un cierre automatico). Deja
     * el valor previo, el nuevo y el motivo en correccion_medicion: el TNS
     * cambia, pero nunca sin rastro.
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    @Transactional
    public Cita corregirDesenlace(Long citaId, EstadoCita nuevoEstado, String motivo, Long usuarioEjecutorId, String ipOrigen) {
        Cita cita = obtener(citaId);
        String previo = cita.getEstado().name() + (cita.isCierreAutomatico() ? " (cierre automatico)" : "");
        correccionMedicionRepositoryPort.guardar(new CorreccionMedicion(null, EntidadCorregida.CITA_DESENLACE, citaId,
                "estado", previo, String.valueOf(nuevoEstado), motivo, usuarioEjecutorId, clock.instant()));
        cita.corregirDesenlace(nuevoEstado, usuarioEjecutorId, clock.instant());
        Cita guardada = citaRepositoryPort.guardar(cita);
        eventPublisher.publishEvent(OperacionAuditadaEvent.exito(usuarioEjecutorId, "CITA_DESENLACE_CORREGIDO", "CITA",
                citaId, "estado=" + previo, "estado=" + nuevoEstado + ";motivo=" + motivo, ipOrigen));
        return guardada;
    }

    /**
     * Confirmar si asistira si esta permitido al propio paciente (seccion
     * 11), a diferencia de "atendida"/"no asistio", que son registro clinico
     * de lo ocurrido y quedan exclusivamente en manos del staff.
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA', 'MEDICO', 'PACIENTE')")
    @Transactional
    public Cita confirmar(Long citaId, Long usuarioEjecutorId, Rol rolEjecutor) {
        Cita cita = obtener(citaId);
        verificarPropiedadSiEsPaciente(cita, usuarioEjecutorId, rolEjecutor);
        cita.confirmar();
        return citaRepositoryPort.guardar(cita);
    }

    private Cita obtener(Long citaId) {
        return citaRepositoryPort.buscarPorId(citaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cita", citaId));
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

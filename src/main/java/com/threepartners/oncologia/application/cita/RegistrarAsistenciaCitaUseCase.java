package com.threepartners.oncologia.application.cita;

import com.threepartners.oncologia.domain.cita.Cita;
import com.threepartners.oncologia.domain.cita.CitaRepositoryPort;
import com.threepartners.oncologia.domain.paciente.Paciente;
import com.threepartners.oncologia.domain.paciente.PacienteRepositoryPort;
import com.threepartners.oncologia.domain.shared.exception.RecursoNoEncontradoException;
import com.threepartners.oncologia.domain.usuario.Rol;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RegistrarAsistenciaCitaUseCase {

    private final CitaRepositoryPort citaRepositoryPort;
    private final PacienteRepositoryPort pacienteRepositoryPort;

    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA', 'MEDICO')")
    @Transactional
    public Cita marcarAtendida(Long citaId) {
        Cita cita = obtener(citaId);
        cita.atender();
        return citaRepositoryPort.guardar(cita);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA', 'MEDICO')")
    @Transactional
    public Cita marcarNoAsistio(Long citaId) {
        Cita cita = obtener(citaId);
        cita.marcarNoAsistio();
        return citaRepositoryPort.guardar(cita);
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

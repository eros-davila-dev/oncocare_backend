package com.threepartners.oncologia.application.cita;

import com.threepartners.oncologia.domain.cita.CitaAgenda;
import com.threepartners.oncologia.domain.cita.CitaRepositoryPort;
import com.threepartners.oncologia.domain.usuario.Rol;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/**
 * Agenda de recepcion: donde se registra si el paciente llego o no (dato del
 * indicador TNS). Un medico ve solo sus propias citas.
 */
@Service
@RequiredArgsConstructor
public class ConsultarAgendaUseCase {

    private final CitaRepositoryPort citaRepositoryPort;
    private final Clock clock;

    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA', 'MEDICO')")
    @Transactional(readOnly = true)
    public List<CitaAgenda> delDia(LocalDate fecha, Long medicoId, Long usuarioId, Rol rol) {
        LocalDate dia = fecha != null ? fecha : LocalDate.now(clock);
        Long medicoFiltro = rol == Rol.MEDICO ? usuarioId : medicoId;
        return citaRepositoryPort.agendaDelDia(dia, medicoFiltro);
    }

    /**
     * Citas de dias anteriores que siguen sin desenlace. Si se quedan asi, el
     * job las cerrara como NO_ASISTIO; esta bandeja permite registrarlas bien
     * antes.
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA', 'MEDICO')")
    @Transactional(readOnly = true)
    public List<CitaAgenda> pendientesDeCierre(Long usuarioId, Rol rol) {
        var pendientes = citaRepositoryPort.pendientesDeCierre(LocalDate.now(clock));
        if (rol != Rol.MEDICO) {
            return pendientes;
        }
        return pendientes.stream().filter(c -> usuarioId.equals(c.cita().getMedicoId())).toList();
    }
}

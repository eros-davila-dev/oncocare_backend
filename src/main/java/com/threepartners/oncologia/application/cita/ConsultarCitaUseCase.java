package com.threepartners.oncologia.application.cita;

import com.threepartners.oncologia.domain.cita.Cita;
import com.threepartners.oncologia.domain.cita.CitaRepositoryPort;
import com.threepartners.oncologia.domain.cita.EstadoCita;
import com.threepartners.oncologia.domain.paciente.Paciente;
import com.threepartners.oncologia.domain.paciente.PacienteRepositoryPort;
import com.threepartners.oncologia.domain.shared.CriterioPaginacion;
import com.threepartners.oncologia.domain.shared.Pagina;
import com.threepartners.oncologia.domain.shared.exception.RecursoNoEncontradoException;
import com.threepartners.oncologia.domain.shared.exception.ValidacionDeNegocioException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class ConsultarCitaUseCase {

    private final CitaRepositoryPort citaRepositoryPort;
    private final PacienteRepositoryPort pacienteRepositoryPort;

    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'RECEPCIONISTA')")
    @Transactional(readOnly = true)
    public Cita porId(Long id) {
        return citaRepositoryPort.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cita", id));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'RECEPCIONISTA')")
    @Transactional(readOnly = true)
    public Pagina<Cita> listar(Long pacienteId, Long medicoId, LocalDate desde, LocalDate hasta, EstadoCita estado, CriterioPaginacion criterio) {
        return citaRepositoryPort.listar(pacienteId, medicoId, desde, hasta, estado, criterio);
    }

    /**
     * Citas del paciente autenticado (seccion 12: el chatbot y el portal
     * "identifican al usuario autenticado" y solo devuelven informacion
     * autorizada de si mismo, nunca de otro paciente por id).
     */
    @PreAuthorize("hasRole('PACIENTE')")
    @Transactional(readOnly = true)
    public Pagina<Cita> misCitas(Long usuarioAutenticadoId, EstadoCita estado, CriterioPaginacion criterio) {
        Paciente paciente = pacienteRepositoryPort.buscarPorUsuarioId(usuarioAutenticadoId)
                .orElseThrow(() -> new ValidacionDeNegocioException("Debes completar tu perfil de paciente antes de consultar tus citas"));
        return citaRepositoryPort.listar(paciente.getId(), null, null, null, estado, criterio);
    }
}

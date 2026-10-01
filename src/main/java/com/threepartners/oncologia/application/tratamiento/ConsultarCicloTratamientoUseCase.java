package com.threepartners.oncologia.application.tratamiento;

import com.threepartners.oncologia.domain.shared.CriterioPaginacion;
import com.threepartners.oncologia.domain.shared.Pagina;
import com.threepartners.oncologia.domain.shared.exception.RecursoNoEncontradoException;
import com.threepartners.oncologia.domain.tratamiento.CicloTratamiento;
import com.threepartners.oncologia.domain.tratamiento.CicloTratamientoRepositoryPort;
import com.threepartners.oncologia.domain.tratamiento.TipoTratamiento;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConsultarCicloTratamientoUseCase {

    private final CicloTratamientoRepositoryPort cicloTratamientoRepositoryPort;

    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'RECEPCIONISTA')")
    @Transactional(readOnly = true)
    public CicloTratamiento porId(Long id) {
        return cicloTratamientoRepositoryPort.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("CicloTratamiento", id));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'RECEPCIONISTA')")
    @Transactional(readOnly = true)
    public List<CicloTratamiento> porPaciente(Long pacienteId) {
        return cicloTratamientoRepositoryPort.listarPorPaciente(pacienteId);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'RECEPCIONISTA')")
    @Transactional(readOnly = true)
    public Pagina<CicloTratamiento> listar(Long pacienteId, TipoTratamiento tipo, CriterioPaginacion criterio) {
        return cicloTratamientoRepositoryPort.listar(pacienteId, tipo, criterio);
    }
}

package com.threepartners.oncologia.application.tratamiento;

import com.threepartners.oncologia.domain.shared.exception.RecursoNoEncontradoException;
import com.threepartners.oncologia.domain.tratamiento.CicloTratamiento;
import com.threepartners.oncologia.domain.tratamiento.CicloTratamientoRepositoryPort;
import com.threepartners.oncologia.domain.tratamiento.EstadoCicloTratamiento;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ActualizarEstadoCicloTratamientoUseCase {

    private final CicloTratamientoRepositoryPort cicloTratamientoRepositoryPort;

    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO')")
    @Transactional
    public CicloTratamiento ejecutar(Long id, EstadoCicloTratamiento nuevoEstado, String observaciones) {
        CicloTratamiento ciclo = cicloTratamientoRepositoryPort.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("CicloTratamiento", id));

        ciclo.setEstado(nuevoEstado);
        if (observaciones != null) {
            ciclo.setObservaciones(observaciones);
        }
        return cicloTratamientoRepositoryPort.guardar(ciclo);
    }
}

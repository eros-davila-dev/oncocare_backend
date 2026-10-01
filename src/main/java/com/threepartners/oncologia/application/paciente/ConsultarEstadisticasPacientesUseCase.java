package com.threepartners.oncologia.application.paciente;

import com.threepartners.oncologia.domain.paciente.EstadisticasPacientes;
import com.threepartners.oncologia.domain.paciente.EstadisticasPacientesRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConsultarEstadisticasPacientesUseCase {

    private final EstadisticasPacientesRepositoryPort estadisticasPacientesRepositoryPort;

    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'RECEPCIONISTA')")
    @Transactional(readOnly = true)
    public EstadisticasPacientes ejecutar() {
        return estadisticasPacientesRepositoryPort.calcular();
    }
}

package com.threepartners.oncologia.infrastructure.in.rest.dto.tratamiento;

import com.threepartners.oncologia.domain.tratamiento.EstadoCicloTratamiento;
import com.threepartners.oncologia.domain.tratamiento.TipoTratamiento;

import java.time.LocalDate;

public record CicloTratamientoResponseDto(
        Long id,
        Long pacienteId,
        TipoTratamiento tipoTratamiento,
        int numeroSesion,
        int totalSesionesEsquema,
        double porcentajeCumplimiento,
        LocalDate fechaSesion,
        Long medicoResponsableId,
        EstadoCicloTratamiento estado,
        String observaciones
) {
}

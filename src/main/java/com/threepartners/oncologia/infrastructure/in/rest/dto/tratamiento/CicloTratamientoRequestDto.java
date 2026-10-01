package com.threepartners.oncologia.infrastructure.in.rest.dto.tratamiento;

import com.threepartners.oncologia.domain.tratamiento.TipoTratamiento;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CicloTratamientoRequestDto(

        @NotNull(message = "El paciente es obligatorio")
        Long pacienteId,

        @NotNull(message = "El tipo de tratamiento es obligatorio")
        TipoTratamiento tipoTratamiento,

        @Min(value = 1, message = "El numero de sesion debe ser al menos 1")
        int numeroSesion,

        @Min(value = 1, message = "El total de sesiones del esquema debe ser al menos 1")
        int totalSesionesEsquema,

        @NotNull(message = "La fecha de sesion es obligatoria")
        LocalDate fechaSesion,

        @NotNull(message = "El medico responsable es obligatorio")
        Long medicoResponsableId,

        String observaciones
) {
}

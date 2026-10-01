package com.threepartners.oncologia.infrastructure.in.rest.dto.cita;

import com.threepartners.oncologia.domain.cita.EstadoCita;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CorregirDesenlaceRequestDto(

        @NotNull(message = "Indique el desenlace correcto")
        EstadoCita estado,

        @NotBlank(message = "El motivo de la correccion es obligatorio")
        @Size(min = 5, max = 500, message = "El motivo debe tener entre 5 y 500 caracteres")
        String motivo
) {
}

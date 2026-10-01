package com.threepartners.oncologia.infrastructure.in.rest.dto.estudio;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnularDatoRequestDto(

        @NotBlank(message = "El motivo de la anulacion es obligatorio")
        @Size(min = 5, max = 500, message = "El motivo debe tener entre 5 y 500 caracteres")
        String motivo
) {
}

package com.threepartners.oncologia.infrastructure.in.rest.dto.chatbot;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ValorarConsultaRequestDto(

        @NotBlank(message = "El identificador de sesion es obligatorio")
        String sesionId,

        @NotNull(message = "La valoracion es obligatoria")
        @Min(value = -1, message = "La valoracion debe ser 1 o -1")
        @Max(value = 1, message = "La valoracion debe ser 1 o -1")
        Integer valor
) {
}

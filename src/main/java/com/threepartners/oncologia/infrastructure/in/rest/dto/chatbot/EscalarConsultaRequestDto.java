package com.threepartners.oncologia.infrastructure.in.rest.dto.chatbot;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EscalarConsultaRequestDto(

        @NotBlank(message = "El identificador de sesion es obligatorio")
        @Size(max = 100, message = "El identificador de sesion no es valido")
        String sesionId
) {
}

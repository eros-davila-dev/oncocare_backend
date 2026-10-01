package com.threepartners.oncologia.infrastructure.in.rest.dto.chatbot;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatbotMensajeRequestDto(

        @NotBlank(message = "El identificador de sesion es obligatorio")
        @Size(max = 100, message = "El identificador de sesion no es valido")
        String sesionId,

        @NotBlank(message = "El mensaje no puede estar vacio")
        @Size(max = 2000, message = "El mensaje no puede superar los 2000 caracteres")
        String mensaje
) {
}

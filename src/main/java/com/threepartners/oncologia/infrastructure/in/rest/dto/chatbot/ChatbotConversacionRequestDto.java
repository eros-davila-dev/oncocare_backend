package com.threepartners.oncologia.infrastructure.in.rest.dto.chatbot;

import jakarta.validation.constraints.NotBlank;

public record ChatbotConversacionRequestDto(
        Long pacienteId,

        @NotBlank(message = "El mensaje del usuario es obligatorio")
        String mensajeUsuario,

        String respuestaBot,

        String intencionDetectada,

        @NotBlank(message = "El canal es obligatorio")
        String canal
) {
}

package com.threepartners.oncologia.infrastructure.in.rest.dto.chatbot;

import java.time.Instant;

public record ChatbotConversacionResponseDto(
        Long id,
        Long pacienteId,
        String mensajeUsuario,
        String respuestaBot,
        String intencionDetectada,
        Instant fecha,
        String canal
) {
}

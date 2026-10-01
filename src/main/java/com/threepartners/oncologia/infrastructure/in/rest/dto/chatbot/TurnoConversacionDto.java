package com.threepartners.oncologia.infrastructure.in.rest.dto.chatbot;

import java.time.Instant;

public record TurnoConversacionDto(Instant fecha, String mensajeUsuario, String respuestaBot, String intencion) {
}

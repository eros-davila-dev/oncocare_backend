package com.threepartners.oncologia.domain.chatbot;

import java.time.Instant;

/**
 * Si un modelo esta apartado (cuota agotada, saturado o inexistente), hasta
 * cuando y por que. Vive en memoria de cada instancia del backend.
 */
public record EstadoModeloGemini(String modelo, boolean disponible, Instant apartadoHasta, String motivo) {
}

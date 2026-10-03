package com.threepartners.oncologia.domain.chatbot;

/** Resultado de probar una clave con un modelo antes o despues de guardarla. */
public record ResultadoPruebaGemini(boolean exitoso, String modelo, long milisegundos, String mensaje) {
}

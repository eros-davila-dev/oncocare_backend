package com.threepartners.oncologia.domain.chatbot;

/** Lo minimo que Gemini necesita saber sobre quien escribe, sin exponerle datos clinicos. */
public record ContextoUsuarioChatbot(boolean autenticado, boolean perfilPacienteCompleto, String nombre) {
}

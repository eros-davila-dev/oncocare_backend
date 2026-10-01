package com.threepartners.oncologia.domain.chatbot;

import java.util.List;

public interface ConversacionChatbotRepositoryPort {

    ConversacionChatbot guardar(ConversacionChatbot conversacion);

    /** Ultimos mensajes de una sesion, en orden cronologico, para dar contexto a Gemini (seccion 41). */
    List<ConversacionChatbot> listarPorSesion(String sesionId, int limite);
}

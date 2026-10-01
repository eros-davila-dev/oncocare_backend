package com.threepartners.oncologia.domain.chatbot;

import java.util.List;

/**
 * Unico punto de contacto con el modelo de lenguaje (seccion 13): Gemini solo
 * interpreta texto y devuelve una intencion + entidades. Nunca recibe
 * credenciales de base de datos ni ejecuta accion alguna por si mismo; quien
 * decide que hacer con la interpretacion es siempre el backend
 * (ChatbotOrquestadorUseCase).
 */
public interface GeminiPort {

    InterpretacionChatbot interpretar(String mensajeUsuario, List<ConversacionChatbot> historialReciente, ContextoUsuarioChatbot contexto);
}

package com.threepartners.oncologia.application.chatbot;

import com.threepartners.oncologia.domain.chatbot.ConversacionChatbot;
import com.threepartners.oncologia.domain.chatbot.ConversacionChatbotRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Invocado por ChatbotWebhookController cuando n8n reenvia el resultado de una
 * interaccion (mensaje del paciente + respuesta generada por el modelo de IA),
 * para dejar trazabilidad de las conversaciones (seccion 8 y RF-07/RF-08).
 */
@Service
@RequiredArgsConstructor
public class RegistrarConversacionUseCase {

    private final ConversacionChatbotRepositoryPort conversacionChatbotRepositoryPort;

    @Transactional
    public ConversacionChatbot ejecutar(Long pacienteId, String mensajeUsuario, String respuestaBot,
                                         String intencionDetectada, String canal) {
        ConversacionChatbot conversacion = ConversacionChatbot.builder()
                .pacienteId(pacienteId)
                .mensajeUsuario(mensajeUsuario)
                .respuestaBot(respuestaBot)
                .intencionDetectada(intencionDetectada)
                .canal(canal)
                .fecha(Instant.now())
                .build();

        return conversacionChatbotRepositoryPort.guardar(conversacion);
    }
}

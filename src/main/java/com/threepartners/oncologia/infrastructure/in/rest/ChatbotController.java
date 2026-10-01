package com.threepartners.oncologia.infrastructure.in.rest;

import com.threepartners.oncologia.application.chatbot.ChatbotOrquestadorUseCase;
import com.threepartners.oncologia.infrastructure.in.rest.dto.chatbot.ChatbotMensajeRequestDto;
import com.threepartners.oncologia.infrastructure.in.rest.dto.chatbot.ChatbotMensajeResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint que consume el widget del navegador (seccion 12). Es publico
 * (permite visitantes anonimos preguntando por horarios/como registrarse),
 * pero si la peticion trae un JWT valido, JwtAuthFilter ya coloco el usuario
 * autenticado en el contexto de seguridad antes de llegar aqui, y el
 * orquestador lo usa para personalizar la conversacion y autorizar acciones
 * sobre citas propias.
 */
@RestController
@RequestMapping("/api/v1/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotOrquestadorUseCase chatbotOrquestadorUseCase;

    @PostMapping("/mensaje")
    public ChatbotMensajeResponseDto enviarMensaje(@Valid @RequestBody ChatbotMensajeRequestDto dto, HttpServletRequest request) {
        String respuesta = chatbotOrquestadorUseCase.procesar(
                dto.sesionId(), dto.mensaje(), AutenticacionActual.usuarioId(), AutenticacionActual.rol(), AutenticacionActual.ipOrigen(request));
        return new ChatbotMensajeResponseDto(respuesta);
    }
}

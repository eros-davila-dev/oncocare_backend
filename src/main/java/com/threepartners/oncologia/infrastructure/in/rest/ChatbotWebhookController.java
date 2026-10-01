package com.threepartners.oncologia.infrastructure.in.rest;

import com.threepartners.oncologia.application.chatbot.RegistrarConversacionUseCase;
import com.threepartners.oncologia.infrastructure.in.rest.dto.chatbot.ChatbotConversacionRequestDto;
import com.threepartners.oncologia.infrastructure.in.rest.dto.chatbot.ChatbotConversacionResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Punto de entrada que consume n8n (seccion 5 y 15) para dejar trazabilidad de
 * cada interaccion del chatbot una vez que el flujo de IA ya genero la
 * respuesta. No es el endpoint al que habla el widget del navegador: el
 * widget conversa directamente contra el webhook de n8n.
 */
@RestController
@RequestMapping("/api/v1/chatbot/webhook")
@RequiredArgsConstructor
public class ChatbotWebhookController {

    private final RegistrarConversacionUseCase registrarConversacionUseCase;
    private final WebhookSecretValidator webhookSecretValidator;

    @PostMapping("/conversaciones")
    public ResponseEntity<ChatbotConversacionResponseDto> registrarConversacion(
            @RequestHeader(value = "X-Webhook-Secret", required = false) String secreto,
            @Valid @RequestBody ChatbotConversacionRequestDto dto) {

        webhookSecretValidator.validar(secreto);

        var conversacion = registrarConversacionUseCase.ejecutar(
                dto.pacienteId(), dto.mensajeUsuario(), dto.respuestaBot(), dto.intencionDetectada(), dto.canal());

        return ResponseEntity.status(HttpStatus.CREATED).body(new ChatbotConversacionResponseDto(
                conversacion.getId(), conversacion.getPacienteId(), conversacion.getMensajeUsuario(),
                conversacion.getRespuestaBot(), conversacion.getIntencionDetectada(), conversacion.getFecha(), conversacion.getCanal()));
    }
}

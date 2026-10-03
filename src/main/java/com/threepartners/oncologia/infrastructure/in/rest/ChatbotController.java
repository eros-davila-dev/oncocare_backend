package com.threepartners.oncologia.infrastructure.in.rest;

import com.threepartners.oncologia.application.chatbot.ChatbotOrquestadorUseCase;
import com.threepartners.oncologia.application.chatbot.InteractuarConsultaChatbotUseCase;
import com.threepartners.oncologia.domain.estudio.CanalConsulta;
import com.threepartners.oncologia.infrastructure.in.rest.dto.chatbot.ChatbotMensajeRequestDto;
import com.threepartners.oncologia.infrastructure.in.rest.dto.chatbot.ChatbotMensajeResponseDto;
import com.threepartners.oncologia.infrastructure.in.rest.dto.chatbot.EscalarConsultaRequestDto;
import com.threepartners.oncologia.infrastructure.in.rest.dto.chatbot.ValorarConsultaRequestDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
    private final InteractuarConsultaChatbotUseCase interactuarConsultaUseCase;

    @PostMapping("/mensaje")
    public ChatbotMensajeResponseDto enviarMensaje(@Valid @RequestBody ChatbotMensajeRequestDto dto, HttpServletRequest request) {
        var respuesta = chatbotOrquestadorUseCase.procesar(dto.sesionId(), dto.mensaje(), CanalConsulta.CHATBOT_WEB,
                AutenticacionActual.usuarioId(), AutenticacionActual.rol(), AutenticacionActual.ipOrigen(request));
        return new ChatbotMensajeResponseDto(respuesta.texto(), respuesta.consultaId(), respuesta.estadoConsulta(),
                respuesta.sugerencias());
    }

    /** Botones de respuesta rapida al abrir el chat (publico, como /mensaje). */
    @GetMapping("/sugerencias")
    public List<String> sugerencias() {
        return chatbotOrquestadorUseCase.sugerenciasIniciales(AutenticacionActual.usuarioId(), AutenticacionActual.rol());
    }

    /** 👍/👎 sobre la respuesta: un 👎 a una respuesta del bot la pasa al personal (NCA). */
    @PostMapping("/consultas/{id}/valoracion")
    public ChatbotMensajeResponseDto valorar(@PathVariable Long id, @Valid @RequestBody ValorarConsultaRequestDto dto) {
        var consulta = interactuarConsultaUseCase.valorar(id, dto.valor(), dto.sesionId());
        String texto = dto.valor() > 0
                ? "¡Gracias! Me alegra haberte ayudado."
                : "Gracias por avisarme. Le pedi a una persona del equipo que revise tu consulta.";
        return new ChatbotMensajeResponseDto(texto, consulta.getId(), consulta.getResultado());
    }

    @PostMapping("/escalar")
    public ChatbotMensajeResponseDto hablarConUnaPersona(@Valid @RequestBody EscalarConsultaRequestDto dto) {
        var consulta = interactuarConsultaUseCase.hablarConUnaPersona(dto.sesionId(), CanalConsulta.CHATBOT_WEB,
                AutenticacionActual.usuarioId(), AutenticacionActual.rol());
        return new ChatbotMensajeResponseDto(
                "Listo, una persona del equipo de la fundacion revisara tu consulta y te respondera lo antes posible.",
                consulta.getId(), consulta.getResultado());
    }
}

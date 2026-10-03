package com.threepartners.oncologia.infrastructure.in.rest.dto.chatbot;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.threepartners.oncologia.domain.estudio.ResultadoConsulta;

import java.util.List;

/**
 * consultaId permite valorar la respuesta (👍/👎) o pedir una persona;
 * estadoConsulta indica si la necesidad quedo resuelta, abierta (null) o
 * escalada; se serializa siempre para que null signifique "abierta".
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record ChatbotMensajeResponseDto(String respuesta, Long consultaId, ResultadoConsulta estadoConsulta,
                                        List<String> sugerencias) {

    /** Respuestas sin sugerencias (valoracion, escalar): lista vacia, nunca null. */
    public ChatbotMensajeResponseDto(String respuesta, Long consultaId, ResultadoConsulta estadoConsulta) {
        this(respuesta, consultaId, estadoConsulta, List.of());
    }
}

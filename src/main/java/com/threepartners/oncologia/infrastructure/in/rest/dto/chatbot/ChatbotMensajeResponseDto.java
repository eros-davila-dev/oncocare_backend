package com.threepartners.oncologia.infrastructure.in.rest.dto.chatbot;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.threepartners.oncologia.domain.estudio.ResultadoConsulta;

/**
 * consultaId permite valorar la respuesta (👍/👎) o pedir una persona;
 * estadoConsulta indica si la necesidad quedo resuelta, abierta (null) o
 * escalada; se serializa siempre para que null signifique "abierta".
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record ChatbotMensajeResponseDto(String respuesta, Long consultaId, ResultadoConsulta estadoConsulta) {
}

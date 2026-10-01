package com.threepartners.oncologia.domain.chatbot;

import com.threepartners.oncologia.domain.estudio.ResultadoConsulta;

/**
 * Lo que recibe el canal (widget web o Telegram): el texto y la consulta a la
 * que pertenece el turno, para poder valorarla (👍/👎) o escalarla.
 */
public record RespuestaChatbot(String texto, Long consultaId, ResultadoConsulta estadoConsulta) {
}

package com.threepartners.oncologia.domain.chatbot;

import java.util.List;

import com.threepartners.oncologia.domain.estudio.ResultadoConsulta;

/**
 * Lo que recibe el canal (widget web o Telegram): el texto y la consulta a la
 * que pertenece el turno, para poder valorarla (👍/👎) o escalarla.
 */
public record RespuestaChatbot(String texto, Long consultaId, ResultadoConsulta estadoConsulta,
                               List<String> sugerencias) {

    public RespuestaChatbot {
        sugerencias = sugerencias == null ? List.of() : List.copyOf(sugerencias);
    }

    public RespuestaChatbot(String texto, Long consultaId, ResultadoConsulta estadoConsulta) {
        this(texto, consultaId, estadoConsulta, List.of());
    }
}

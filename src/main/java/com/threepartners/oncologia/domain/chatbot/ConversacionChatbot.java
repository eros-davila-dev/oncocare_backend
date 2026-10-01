package com.threepartners.oncologia.domain.chatbot;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversacionChatbot {

    private Long id;
    private Long pacienteId;
    /** Consulta (necesidad) a la que pertenece este turno: une la conversacion con el indicador NCA. */
    private Long consultaId;
    private String sesionId;
    private String mensajeUsuario;
    private String respuestaBot;
    private String intencionDetectada;
    private Instant fecha;
    private String canal;
}

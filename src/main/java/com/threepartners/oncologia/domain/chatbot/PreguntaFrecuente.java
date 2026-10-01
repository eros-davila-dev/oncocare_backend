package com.threepartners.oncologia.domain.chatbot;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Informacion oficial de la fundacion (horarios, requisitos, ubicacion...).
 * Es la unica fuente que el chatbot puede usar para responder preguntas
 * generales: lo que no esta aqui, no lo inventa y lo escala al personal.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreguntaFrecuente {

    private Long id;
    private String pregunta;
    private String respuesta;
    private String categoria;
    private int orden;
    private boolean activa;
}

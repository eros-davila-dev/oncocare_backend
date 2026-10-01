package com.threepartners.oncologia.domain.chatbot;

import java.util.List;

/**
 * Lo minimo que Gemini necesita saber sobre quien escribe (sin exponerle
 * datos clinicos) y la informacion oficial de la fundacion con la que puede
 * responder preguntas generales.
 */
public record ContextoUsuarioChatbot(boolean autenticado, boolean perfilPacienteCompleto, String nombre,
                                     List<PreguntaFrecuente> preguntasFrecuentes) {

    public ContextoUsuarioChatbot(boolean autenticado, boolean perfilPacienteCompleto, String nombre) {
        this(autenticado, perfilPacienteCompleto, nombre, List.of());
    }

    public ContextoUsuarioChatbot conConocimiento(List<PreguntaFrecuente> preguntas) {
        return new ContextoUsuarioChatbot(autenticado, perfilPacienteCompleto, nombre, preguntas);
    }
}

package com.threepartners.oncologia.domain.chatbot;

import java.util.List;

/**
 * Lo que el chatbot usa en este momento: lo guardado en la intranet tiene
 * prioridad y, lo que falte, sale de las variables de entorno.
 */
public record ConfiguracionGeminiVigente(String apiKey, OrigenConfiguracionGemini origenClave,
                                         boolean claveGuardadaIlegible, List<String> modelos,
                                         OrigenConfiguracionGemini origenModelos) {

    public boolean tieneClave() {
        return apiKey != null && !apiKey.isBlank();
    }

    /** Solo los ultimos 4 caracteres: la clave completa nunca sale del backend. */
    public String claveEnmascarada() {
        if (!tieneClave()) {
            return null;
        }
        return apiKey.length() <= 4 ? "****" : "****" + apiKey.substring(apiKey.length() - 4);
    }
}

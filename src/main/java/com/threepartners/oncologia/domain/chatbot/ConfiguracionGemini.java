package com.threepartners.oncologia.domain.chatbot;

import java.time.Instant;
import java.util.List;

/**
 * Configuracion del asistente guardada por el administrador desde la
 * intranet. apiKey es null si el administrador no cargo una (se usa la
 * variable de entorno) o si claveIlegible: la clave guardada no se pudo
 * descifrar, normalmente porque cambio APP_CLAVE_CIFRADO, y hay que volver a
 * ingresarla.
 */
public record ConfiguracionGemini(String apiKey, boolean claveIlegible, List<String> modelos,
                                  Long actualizadoPor, Instant actualizadoEn) {

    public ConfiguracionGemini {
        modelos = modelos == null ? List.of() : List.copyOf(modelos);
    }

    public boolean tieneClave() {
        return apiKey != null && !apiKey.isBlank();
    }
}

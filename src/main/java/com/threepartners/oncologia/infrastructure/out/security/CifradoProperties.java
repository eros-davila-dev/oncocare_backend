package com.threepartners.oncologia.infrastructure.out.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Clave para cifrar los secretos que se guardan en la base (por ahora, la
 * clave de API de Gemini cargada desde la intranet). Si esta vacia se deriva
 * de JWT_SECRET; conviene una propia, porque si se cambia JWT_SECRET (para
 * cerrar todas las sesiones) los secretos guardados dejan de poder leerse.
 */
@ConfigurationProperties(prefix = "app.cifrado")
public record CifradoProperties(String clave) {
}

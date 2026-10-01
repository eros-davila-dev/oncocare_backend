package com.threepartners.oncologia.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * URL base del frontend Angular, usada para construir los enlaces de
 * verificacion de correo y restablecimiento de contrasena que se envian por
 * el workflow de notificaciones de n8n.
 */
@ConfigurationProperties(prefix = "app.frontend")
public record FrontendProperties(String baseUrl) {
}

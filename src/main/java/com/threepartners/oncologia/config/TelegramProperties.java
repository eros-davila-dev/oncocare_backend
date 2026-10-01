package com.threepartners.oncologia.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * El backend nunca guarda el token del bot (vive solo en las credenciales de
 * n8n). Solo necesita el usuario publico del bot para armar el enlace de
 * vinculacion t.me/<bot>?start=<token>.
 *
 * @param botUsername          usuario publico del bot, sin "@"
 * @param minutosValidezEnlace vigencia del enlace o QR de vinculacion
 */
@ConfigurationProperties(prefix = "app.telegram")
public record TelegramProperties(String botUsername, int minutosValidezEnlace) {

    public TelegramProperties {
        if (minutosValidezEnlace <= 0) {
            minutosValidezEnlace = 30;
        }
        if (botUsername != null) {
            botUsername = botUsername.strip().replaceFirst("^@", "");
        }
    }

    public boolean configurado() {
        return botUsername != null && !botUsername.isBlank();
    }
}

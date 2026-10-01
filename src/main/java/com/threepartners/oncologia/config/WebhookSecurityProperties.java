package com.threepartners.oncologia.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Secreto compartido para autenticar los webhooks expuestos a n8n (seccion 12,
 * punto 10 y seccion 15). Los dispositivos externos usan su propia credencial
 * por dispositivo (ver DeviceWebhookController), no este secreto.
 */
@ConfigurationProperties(prefix = "app.webhook")
public record WebhookSecurityProperties(String n8nSecret) {
}

package com.threepartners.oncologia.infrastructure.out.notification;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.n8n")
public record N8nProperties(String baseUrl, String webhookSecret) {
}

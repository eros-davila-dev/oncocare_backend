package com.threepartners.oncologia.infrastructure.in.rest;

import com.threepartners.oncologia.config.WebhookSecurityProperties;
import com.threepartners.oncologia.domain.shared.exception.CredencialesInvalidasException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Autenticacion de los webhooks expuestos a n8n mediante un header secreto
 * (seccion 12, punto 10 y seccion 15). Estas rutas estan marcadas como
 * permitAll en SecurityConfig porque n8n no maneja JWT de usuario; en su lugar
 * este validador exige el secreto compartido antes de procesar la solicitud.
 */
@Component
@RequiredArgsConstructor
public class WebhookSecretValidator {

    private static final String HEADER = "X-Webhook-Secret";

    private final WebhookSecurityProperties webhookSecurityProperties;

    public void validar(String secretoRecibido) {
        if (secretoRecibido == null || !comparacionSegura(secretoRecibido, webhookSecurityProperties.n8nSecret())) {
            throw new CredencialesInvalidasException();
        }
    }

    public String header() {
        return HEADER;
    }

    private boolean comparacionSegura(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}

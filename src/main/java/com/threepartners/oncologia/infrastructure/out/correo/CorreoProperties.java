package com.threepartners.oncologia.infrastructure.out.correo;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuracion de correo (app.correo). Las variables de entorno siguen los
 * nombres que ya usa la fundacion en sus otros sistemas: MAIL_FLAG_QUIPU
 * (API), MAIL_FLAG_SMTP, MAIL_API_URL, MAIL_API_KEY, MAIL_USERNAME...
 */
@ConfigurationProperties(prefix = "app.correo")
public record CorreoProperties(String remitente, String nombreRemitente, String logoUrl, Api api, Smtp smtp) {

    public CorreoProperties {
        api = api != null ? api : new Api(false, null, null);
        smtp = smtp != null ? smtp : new Smtp(false);
    }

    public record Api(boolean habilitado, String url, String clave) {
    }

    public record Smtp(boolean habilitado) {
    }
}

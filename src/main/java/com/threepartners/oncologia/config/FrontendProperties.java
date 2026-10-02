package com.threepartners.oncologia.config;

import com.threepartners.oncologia.domain.usuario.Rol;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

/**
 * URLs publicas de las dos aplicaciones Angular, usadas para construir los
 * enlaces de los correos (verificacion, restablecimiento, bienvenida). El
 * paciente usa el portal; el personal, la intranet.
 */
@ConfigurationProperties(prefix = "app.frontend")
public record FrontendProperties(String baseUrl, String intranetUrl) {

    @ConstructorBinding
    public FrontendProperties {
        intranetUrl = intranetUrl == null || intranetUrl.isBlank() ? baseUrl : intranetUrl;
    }

    /** Solo el portal (tests y llamadores que no distinguen aplicacion). */
    public FrontendProperties(String baseUrl) {
        this(baseUrl, baseUrl);
    }

    /** URL base de la aplicacion en la que inicia sesion una cuenta con ese rol. */
    public String urlDeAplicacion(Rol rol) {
        return rol == Rol.PACIENTE ? baseUrl : intranetUrl;
    }
}

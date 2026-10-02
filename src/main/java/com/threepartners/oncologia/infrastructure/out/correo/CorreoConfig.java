package com.threepartners.oncologia.infrastructure.out.correo;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.ArrayList;
import java.util.List;

/**
 * Arma la lista de proveedores segun las banderas. Falla al arrancar si una
 * bandera esta activa sin su configuracion: es mejor enterarse en el
 * despliegue que con el primer paciente que no recibe su correo.
 */
@Slf4j
@Configuration
public class CorreoConfig {

    @Bean
    ServicioCorreo servicioCorreo(CorreoProperties propiedades, ObjectProvider<JavaMailSender> mailSender,
                                  ObjectMapper objectMapper) {
        List<ProveedorCorreo> proveedores = new ArrayList<>();
        if (propiedades.api().habilitado()) {
            proveedores.add(new ApiCorreoProveedor(propiedades.api().url(), propiedades.api().clave(), objectMapper));
        }
        if (propiedades.smtp().habilitado()) {
            JavaMailSender smtp = mailSender.getIfAvailable();
            if (smtp == null) {
                throw new IllegalStateException("MAIL_FLAG_SMTP=true requiere MAIL_HOST (y MAIL_USERNAME / MAIL_PASSWORD)");
            }
            proveedores.add(new SmtpCorreoProveedor(smtp));
        }
        if (!proveedores.isEmpty() && (propiedades.remitente() == null || propiedades.remitente().isBlank())) {
            throw new IllegalStateException("Defina el remitente del correo: MAIL_FROM_EMAIL o MAIL_USERNAME");
        }

        var servicio = new ServicioCorreo(proveedores, propiedades.remitente(), propiedades.nombreRemitente());
        if (servicio.habilitado()) {
            log.info("Correo: proveedores activos en orden {}", servicio.proveedoresActivos());
        } else {
            log.warn("Correo: ningun proveedor activo (MAIL_FLAG_QUIPU / MAIL_FLAG_SMTP); los correos quedaran "
                    + "pendientes en el outbox hasta agotar los reintentos");
        }
        return servicio;
    }
}

package com.threepartners.oncologia.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Interruptor del autoservicio de registro de pacientes (seccion 7).
 * Deliberadamente en false por defecto: crear una cuenta sin poder enviar el
 * correo de verificacion (SMTP/n8n aun no configurado) dejaria cuentas
 * huerfanas en PENDIENTE_VERIFICACION sin forma de activarlas. Activar con
 * REGISTRO_PACIENTES_HABILITADO=true una vez el workflow de correo este listo.
 */
@ConfigurationProperties(prefix = "app.registro")
public record RegistroProperties(boolean pacientesHabilitado) {
}

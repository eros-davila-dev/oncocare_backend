package com.threepartners.oncologia.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param correoHabilitado programa recordatorios por correo (72 h y 24 h) ademas
 *                         del canal principal. Apagado por defecto: la
 *                         intervencion descrita en la tesis es Telegram; si se
 *                         activa durante el postest, hay que describirlo en la
 *                         tesis antes de empezar.
 */
@ConfigurationProperties(prefix = "app.recordatorios")
public record RecordatoriosProperties(boolean correoHabilitado) {
}

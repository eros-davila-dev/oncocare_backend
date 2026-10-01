package com.threepartners.oncologia.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Parametros del modulo de estudio (indicadores de la tesis).
 *
 * @param minutosMaximosRegistro tiempo tras el cual una sesion de medicion
 *                               abierta y nunca guardada se marca ABANDONADA
 *                               (no entra al TPR).
 */
@ConfigurationProperties(prefix = "app.estudio")
public record EstudioProperties(int minutosMaximosRegistro) {

    public EstudioProperties {
        if (minutosMaximosRegistro <= 0) {
            minutosMaximosRegistro = 60;
        }
    }
}

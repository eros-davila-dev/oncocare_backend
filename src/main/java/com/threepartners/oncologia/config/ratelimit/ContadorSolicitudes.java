package com.threepartners.oncologia.config.ratelimit;

import java.time.Duration;

/**
 * Contador de ventana fija para el rate limit. Devuelve cuantas solicitudes
 * lleva la clave en la ventana actual, incluida la que se esta contando.
 */
public interface ContadorSolicitudes {

    long incrementar(String clave, Duration ventana);
}

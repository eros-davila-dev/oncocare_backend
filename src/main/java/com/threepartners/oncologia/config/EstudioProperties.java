package com.threepartners.oncologia.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

import java.time.DayOfWeek;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Parametros del modulo de estudio (indicadores de la tesis).
 *
 * @param minutosMaximosRegistro tiempo tras el cual una sesion de medicion
 *                               abierta y nunca guardada se marca ABANDONADA
 *                               (no entra al TPR).
 * @param diasSesion             dias de observacion de cada etapa (tesis v8:
 *                               lunes, miercoles y viernes).
 */
@ConfigurationProperties(prefix = "app.estudio")
public record EstudioProperties(int minutosMaximosRegistro, List<DayOfWeek> diasSesion) {

    @ConstructorBinding
    public EstudioProperties {
        if (minutosMaximosRegistro <= 0) {
            minutosMaximosRegistro = 60;
        }
        diasSesion = diasSesion == null || diasSesion.isEmpty()
                ? List.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
                : List.copyOf(diasSesion);
    }

    public EstudioProperties(int minutosMaximosRegistro) {
        this(minutosMaximosRegistro, null);
    }

    public Set<DayOfWeek> diasDeSesion() {
        return EnumSet.copyOf(diasSesion);
    }
}

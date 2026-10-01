package com.threepartners.oncologia.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cierre de citas sin desenlace (indicador TNS).
 *
 * @param horasCierreAutomatico horas despues de la cita en que, si nadie
 *                              registro si el paciente llego, se marca
 *                              NO_ASISTIO automaticamente (corregible).
 */
@ConfigurationProperties(prefix = "app.agenda")
public record AgendaProperties(int horasCierreAutomatico) {

    public AgendaProperties {
        if (horasCierreAutomatico <= 0) {
            horasCierreAutomatico = 48;
        }
    }
}

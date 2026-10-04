package com.threepartners.oncologia.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

/**
 * Nombre con el que el sistema se presenta (asistente virtual, correos,
 * documentacion de la API). Es configurable y por defecto generico para no
 * identificar a la institucion del estudio (anonimato de la Guia V07).
 */
@ConfigurationProperties(prefix = "app.institucion")
public record InstitucionProperties(String nombre) {

    public static final String NOMBRE_POR_DEFECTO = "Fundación Oncológica";

    @ConstructorBinding
    public InstitucionProperties {
        nombre = nombre == null || nombre.isBlank() ? NOMBRE_POR_DEFECTO : nombre.strip();
    }

    public static InstitucionProperties porDefecto() {
        return new InstitucionProperties(NOMBRE_POR_DEFECTO);
    }
}

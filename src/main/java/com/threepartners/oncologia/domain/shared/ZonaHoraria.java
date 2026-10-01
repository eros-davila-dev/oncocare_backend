package com.threepartners.oncologia.domain.shared;

import java.time.ZoneId;

/**
 * Zona horaria de la fundacion. Toda decision que dependa del "dia" (a que
 * fase del estudio pertenece un evento, si una cita ya paso, cuando enviar un
 * recordatorio) se toma en hora de Lima, nunca en la zona del servidor, que en
 * un contenedor suele ser UTC.
 */
public final class ZonaHoraria {

    public static final ZoneId LIMA = ZoneId.of("America/Lima");

    private ZonaHoraria() {
    }
}

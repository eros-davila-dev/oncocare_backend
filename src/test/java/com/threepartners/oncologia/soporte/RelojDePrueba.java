package com.threepartners.oncologia.soporte;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

/**
 * Reloj que un test puede fijar y adelantar, para probar reglas que dependen
 * del tiempo (recordatorios, cierres automaticos, ventanas de reformulacion).
 */
public final class RelojDePrueba extends Clock {

    private Instant ahora;
    private final ZoneId zona;

    public RelojDePrueba(Instant inicio, ZoneId zona) {
        this.ahora = inicio;
        this.zona = zona;
    }

    public void avanzar(Duration duracion) {
        ahora = ahora.plus(duracion);
    }

    public void fijar(Instant instante) {
        ahora = instante;
    }

    @Override
    public ZoneId getZone() {
        return zona;
    }

    @Override
    public Clock withZone(ZoneId nuevaZona) {
        return new RelojDePrueba(ahora, nuevaZona);
    }

    @Override
    public Instant instant() {
        return ahora;
    }
}

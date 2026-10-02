package com.threepartners.oncologia.config.ratelimit;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Ventana fija en memoria: correcta con una sola instancia del backend. Cada
 * cierto numero de solicitudes descarta las ventanas vencidas, para que el
 * mapa no crezca sin limite con IPs que no vuelven.
 */
public class ContadorSolicitudesEnMemoria implements ContadorSolicitudes {

    private static final int LIMPIEZA_CADA = 1_000;

    private final ConcurrentHashMap<String, Ventana> ventanas = new ConcurrentHashMap<>();
    private final AtomicLong solicitudes = new AtomicLong();
    private final Clock clock;

    public ContadorSolicitudesEnMemoria(Clock clock) {
        this.clock = clock;
    }

    @Override
    public long incrementar(String clave, Duration ventana) {
        long ahora = clock.millis();
        long duracion = ventana.toMillis();
        if (solicitudes.incrementAndGet() % LIMPIEZA_CADA == 0) {
            ventanas.entrySet().removeIf(e -> ahora - e.getValue().inicioMilis() > duracion);
        }
        Ventana actual = ventanas.compute(clave, (k, previa) ->
                previa == null || ahora - previa.inicioMilis() > duracion ? new Ventana(ahora, new AtomicInteger()) : previa);
        return actual.contador().incrementAndGet();
    }

    int ventanasActivas() {
        return ventanas.size();
    }

    private record Ventana(long inicioMilis, AtomicInteger contador) {
    }
}

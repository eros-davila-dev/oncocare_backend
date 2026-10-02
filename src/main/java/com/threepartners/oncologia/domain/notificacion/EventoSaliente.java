package com.threepartners.oncologia.domain.notificacion;

import java.time.Duration;
import java.util.Map;

/**
 * Aviso pendiente de entregar al orquestador de mensajeria (outbox). La
 * politica de reintentos vive aqui para poder probarla sin base de datos.
 */
public record EventoSaliente(Long id, String destino, Map<String, Object> payload, int intentos) {

    /** 6 intentos con espera 1, 2, 4, 8, 16 min: cubre ~30 min de n8n caido. */
    public static final int MAXIMO_INTENTOS = 6;
    private static final Duration ESPERA_MAXIMA = Duration.ofMinutes(30);

    /** Intentos tras registrar el fallo que acaba de ocurrir. */
    public int intentosTrasFallo() {
        return intentos + 1;
    }

    public boolean agotoReintentos() {
        return intentosTrasFallo() >= MAXIMO_INTENTOS;
    }

    /** Espera exponencial antes del siguiente intento (1, 2, 4... min, con tope). */
    public Duration esperaAntesDelSiguienteIntento() {
        Duration espera = Duration.ofMinutes(1L << Math.min(intentos, 10));
        return espera.compareTo(ESPERA_MAXIMA) > 0 ? ESPERA_MAXIMA : espera;
    }
}

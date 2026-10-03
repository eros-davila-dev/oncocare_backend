package com.threepartners.oncologia.infrastructure.out.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reparte los mensajes del chatbot entre los modelos de Gemini del plan
 * gratuito: cuando uno agota su cuota se aparta y se usa el siguiente, sin
 * que el paciente lo note. Cada modelo tiene su propia cuota (p. ej. 5
 * solicitudes por minuto y 20 por dia), asi que sumarlos multiplica la
 * capacidad diaria del asistente.
 *
 * Cuanto se aparta cada modelo depende de lo que responde Google:
 * - cuota DIARIA agotada: hasta la medianoche del Pacifico, cuando Google la reinicia;
 * - cuota por MINUTO: el tiempo que indica Google (RetryInfo) o 60 s;
 * - modelo inexistente o sin acceso: 6 h (probablemente un nombre mal escrito);
 * - Google saturado o sin respuesta: 30 s, y el doble por cada fallo seguido
 *   (1 min, 2 min... hasta 15 min); vuelve a 30 s cuando el modelo responde.
 *   Asi, si un modelo pasa horas saturado, los mensajes no pierden tiempo con el.
 *
 * El estado vive en memoria: con varias instancias cada una lo aprende por
 * su cuenta, lo que solo cuesta un intento fallido por modelo.
 */
@Slf4j
@Component
public class RotacionModelosGemini {

    static final ZoneId ZONA_CUOTA_DIARIA = ZoneId.of("America/Los_Angeles");
    static final Duration ESPERA_POR_MINUTO = Duration.ofSeconds(60);
    static final Duration ESPERA_MODELO_INEXISTENTE = Duration.ofHours(6);
    static final Duration ESPERA_SATURADO = Duration.ofSeconds(30);
    static final Duration ESPERA_SATURADO_MAXIMA = Duration.ofMinutes(15);
    private static final Pattern SEGUNDOS = Pattern.compile("(\\d+(?:\\.\\d+)?)s");

    private final Map<String, Instant> apartadosHasta = new ConcurrentHashMap<>();
    private final Map<String, Integer> saturacionesSeguidas = new ConcurrentHashMap<>();
    private final Clock clock;
    private final ObjectMapper objectMapper;

    public RotacionModelosGemini(Clock clock, ObjectMapper objectMapper) {
        this.clock = clock;
        this.objectMapper = objectMapper;
    }

    /** Modelos utilizables ahora, en el orden de preferencia configurado. */
    public List<String> disponibles(List<String> enOrden) {
        Instant ahora = clock.instant();
        return enOrden.stream()
                .filter(m -> {
                    Instant hasta = apartadosHasta.get(m);
                    return hasta == null || !ahora.isBefore(hasta);
                })
                .toList();
    }

    /** Respuesta 429: aparta el modelo por un minuto o hasta el reinicio diario. */
    public void cuotaAgotada(String modelo, String cuerpoError) {
        Instant ahora = clock.instant();
        if (esCuotaDiaria(cuerpoError)) {
            Instant reinicio = LocalDate.now(clock.withZone(ZONA_CUOTA_DIARIA)).plusDays(1)
                    .atStartOfDay(ZONA_CUOTA_DIARIA).toInstant();
            apartar(modelo, reinicio, "agoto su cuota diaria");
        } else {
            apartar(modelo, ahora.plus(esperaIndicada(cuerpoError).orElse(ESPERA_POR_MINUTO)),
                    "alcanzo su limite por minuto");
        }
    }

    public void modeloInexistente(String modelo) {
        apartar(modelo, clock.instant().plus(ESPERA_MODELO_INEXISTENTE), "no existe o la clave no tiene acceso");
    }

    public void saturado(String modelo, String detalle) {
        int seguidas = saturacionesSeguidas.merge(modelo, 1, Integer::sum);
        Duration espera = ESPERA_SATURADO.multipliedBy(1L << Math.min(seguidas - 1, 10));
        if (espera.compareTo(ESPERA_SATURADO_MAXIMA) > 0) {
            espera = ESPERA_SATURADO_MAXIMA;
        }
        apartar(modelo, clock.instant().plus(espera), "no respondio o esta saturado (" + detalle + ")");
    }

    /** El modelo respondio: deja de contar sus fallos seguidos. */
    public void exito(String modelo) {
        saturacionesSeguidas.remove(modelo);
    }

    private void apartar(String modelo, Instant hasta, String motivo) {
        apartadosHasta.put(modelo, hasta);
        log.warn("Gemini: el modelo {} {}; se usa el siguiente hasta {}", modelo, motivo, hasta);
    }

    private boolean esCuotaDiaria(String cuerpo) {
        for (JsonNode detalle : detalles(cuerpo)) {
            for (JsonNode violacion : detalle.path("violations")) {
                if (violacion.path("quotaId").asText("").contains("PerDay")) {
                    return true;
                }
            }
        }
        return false;
    }

    private Optional<Duration> esperaIndicada(String cuerpo) {
        for (JsonNode detalle : detalles(cuerpo)) {
            String espera = detalle.path("retryDelay").asText("");
            Matcher m = SEGUNDOS.matcher(espera);
            if (m.matches()) {
                return Optional.of(Duration.ofMillis((long) (Double.parseDouble(m.group(1)) * 1000)).plusSeconds(1));
            }
        }
        return Optional.empty();
    }

    private Iterable<JsonNode> detalles(String cuerpo) {
        try {
            return objectMapper.readTree(cuerpo == null ? "{}" : cuerpo).path("error").path("details");
        } catch (Exception e) {
            return List.of();
        }
    }
}

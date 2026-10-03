package com.threepartners.oncologia.infrastructure.out.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Credenciales y modelos de Gemini (seccion 13). apiKey se deja vacio por
 * defecto a proposito: sin una clave real, GeminiRestClientAdapter degrada a
 * una respuesta de "asistente no disponible" en vez de fallar la peticion.
 *
 * model es el modelo preferido; modelos, los de reserva en orden. Cuando uno
 * agota su cuota gratuita (por minuto o por dia) se usa el siguiente, ver
 * RotacionModelosGemini.
 */
@ConfigurationProperties(prefix = "app.gemini")
public record GeminiProperties(String apiKey, String model, List<String> modelos, String baseUrl,
                               Duration timeoutPorModelo, Duration presupuestoTotal) {

    /**
     * Reserva por defecto: primero los Flash (20 solicitudes/dia cada uno en el
     * plan gratuito) y al final los Flash-Lite (hasta 500/dia).
     */
    public static final List<String> MODELOS_POR_DEFECTO = List.of(
            "gemini-3.8-flash", "gemini-3.7-flash", "gemini-3.6-flash", "gemini-3.5-flash",
            "gemini-3-flash-preview", "gemini-2.5-flash",
            "gemini-3.5-flash-lite", "gemini-3.1-flash-lite", "gemini-2.5-flash-lite");

    @ConstructorBinding
    public GeminiProperties {
        // Una variable GEMINI_MODELS vacia (comun en .env y docker compose) significa "la lista por defecto".
        List<String> configurados = modelos == null ? List.of()
                : modelos.stream().filter(m -> m != null && !m.isBlank()).toList();
        modelos = configurados.isEmpty() ? MODELOS_POR_DEFECTO : configurados;
        // Un chat no puede hacer esperar al paciente: tope por modelo y por mensaje.
        timeoutPorModelo = timeoutPorModelo != null ? timeoutPorModelo : Duration.ofSeconds(10);
        presupuestoTotal = presupuestoTotal != null ? presupuestoTotal : Duration.ofSeconds(20);
    }

    public GeminiProperties(String apiKey, String model, List<String> modelos, String baseUrl) {
        this(apiKey, model, modelos, baseUrl, null, null);
    }

    public GeminiProperties(String apiKey, String model, String baseUrl) {
        this(apiKey, model, List.of(), baseUrl);
    }

    /** El preferido primero y luego los de reserva, sin repetidos ni vacios. */
    public List<String> modelosEnOrden() {
        Set<String> orden = new LinkedHashSet<>();
        if (model != null && !model.isBlank()) {
            orden.add(model.trim());
        }
        modelos.stream().filter(m -> m != null && !m.isBlank()).map(String::trim).forEach(orden::add);
        return List.copyOf(orden);
    }
}

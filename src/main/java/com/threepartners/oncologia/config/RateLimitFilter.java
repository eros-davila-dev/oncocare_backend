package com.threepartners.oncologia.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate limiting basico por IP sobre el login y el webhook del chatbot (seccion
 * 12, punto 6), para mitigar fuerza bruta y abuso conversacional. Implementacion
 * en memoria de ventana fija; para despliegues multi-instancia se recomienda
 * moverla a Redis, pero para el alcance de este sistema resuelve el riesgo real.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int LIMITE_LOGIN = 10;
    private static final int LIMITE_CHATBOT = 60;
    private static final int LIMITE_CHATBOT_MENSAJE = 20;
    private static final long VENTANA_MILIS = 60_000;

    private final ConcurrentHashMap<String, Ventana> contadores = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        Integer limite = null;

        if (path.startsWith("/api/v1/auth/login")) {
            limite = LIMITE_LOGIN;
        } else if (path.startsWith("/api/v1/chatbot/webhook")) {
            limite = LIMITE_CHATBOT;
        } else if (path.startsWith("/api/v1/chatbot/mensaje")) {
            // Publico y cada llamada consume cuota de Gemini (seccion 12,
            // punto 6): limite mas estricto que el webhook interno de n8n.
            limite = LIMITE_CHATBOT_MENSAJE;
        }

        if (limite != null && excedeLimite(claveDe(request, path), limite)) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write("""
                    {"status":429,"code":"DEMASIADAS_SOLICITUDES","message":"Ha excedido el limite de solicitudes, intente nuevamente en un minuto"}""");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String claveDe(HttpServletRequest request, String path) {
        return path + "|" + request.getRemoteAddr();
    }

    private boolean excedeLimite(String clave, int limite) {
        long ahora = Instant.now().toEpochMilli();
        Ventana ventana = contadores.compute(clave, (k, actual) -> {
            if (actual == null || ahora - actual.inicioMilis() > VENTANA_MILIS) {
                return new Ventana(ahora, new AtomicInteger(0));
            }
            return actual;
        });
        return ventana.contador().incrementAndGet() > limite;
    }

    private record Ventana(long inicioMilis, AtomicInteger contador) {
    }
}

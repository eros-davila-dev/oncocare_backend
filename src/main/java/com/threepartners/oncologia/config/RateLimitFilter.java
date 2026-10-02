package com.threepartners.oncologia.config;

import com.threepartners.oncologia.config.ratelimit.ContadorSolicitudes;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

/**
 * Rate limiting basico por IP sobre el login y el webhook del chatbot (seccion
 * 12, punto 6), para mitigar fuerza bruta y abuso conversacional. Ventana fija
 * de un minuto; el conteo vive en memoria o en Redis segun
 * app.rate-limit.almacen (ver RateLimitConfig), asi el limite se respeta
 * aunque haya varias instancias del backend.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int LIMITE_LOGIN = 10;
    private static final int LIMITE_CHATBOT = 60;
    private static final int LIMITE_CHATBOT_MENSAJE = 20;
    private static final Duration VENTANA = Duration.ofMinutes(1);

    private final ContadorSolicitudes contadorSolicitudes;

    public RateLimitFilter(ContadorSolicitudes contadorSolicitudes) {
        this.contadorSolicitudes = contadorSolicitudes;
    }

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

        if (limite != null && contadorSolicitudes.incrementar(claveDe(request, path), VENTANA) > limite) {
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(VENTANA.toSeconds()));
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
}

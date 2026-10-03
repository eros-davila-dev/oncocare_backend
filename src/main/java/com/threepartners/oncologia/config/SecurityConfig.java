package com.threepartners.oncologia.config;

import com.threepartners.oncologia.infrastructure.in.rest.advice.JsonAccessDeniedHandler;
import com.threepartners.oncologia.infrastructure.in.rest.advice.JsonAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final RateLimitFilter rateLimitFilter;
    private final JsonAuthenticationEntryPoint jsonAuthenticationEntryPoint;
    private final JsonAccessDeniedHandler jsonAccessDeniedHandler;
    private final CorsConfigurationSource corsConfigurationSource;

    private static final String[] RUTAS_PUBLICAS = {
            "/api/v1/auth/**",
            "/api/v1/chatbot/webhook/**",
            "/api/v1/chatbot/mensaje",
            "/api/v1/chatbot/sugerencias",
            "/api/v1/chatbot/escalar",
            "/api/v1/chatbot/consultas/*/valoracion",
            "/api/v1/devices/webhook/**",
            "/api/v1/notificaciones/callback",
            // Contrato con n8n: cada endpoint exige X-Webhook-Secret (IntegracionN8nController).
            "/api/v1/integraciones/**",
            "/actuator/health",
            // Solo conteos sin datos personales. En produccion el actuator escucha en
            // un puerto interno no publicado y el proxy nunca enruta /actuator.
            "/actuator/prometheus",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(RUTAS_PUBLICAS).permitAll()
                        // Base de conocimiento: el portal la muestra a visitantes anonimos.
                        .requestMatchers(HttpMethod.GET, "/api/v1/preguntas-frecuentes").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(jsonAuthenticationEntryPoint)
                        .accessDeniedHandler(jsonAccessDeniedHandler))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(rateLimitFilter, JwtAuthFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

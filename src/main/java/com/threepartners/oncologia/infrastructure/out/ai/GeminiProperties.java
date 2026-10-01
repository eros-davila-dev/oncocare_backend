package com.threepartners.oncologia.infrastructure.out.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Credenciales y modelo de Gemini (seccion 13). apiKey se deja vacio por
 * defecto a proposito: sin una clave real, GeminiRestClientAdapter degrada a
 * una respuesta de "asistente no disponible" en vez de fallar la peticion.
 * Consigue una clave gratuita en https://aistudio.google.com/apikey.
 */
@ConfigurationProperties(prefix = "app.gemini")
public record GeminiProperties(String apiKey, String model, String baseUrl) {
}

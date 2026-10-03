package com.threepartners.oncologia.infrastructure.in.rest.dto.chatbot;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.threepartners.oncologia.domain.chatbot.OrigenConfiguracionGemini;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

/**
 * DTO de Configuracion > Asistente IA. La clave completa entra (al guardar o
 * probar) pero nunca sale: la respuesta solo trae sus ultimos 4 caracteres.
 *
 * Los patrones no son cosmeticos: el modelo va dentro de la URL de Google y
 * la clave en una cabecera HTTP, asi que se rechazan espacios, saltos de
 * linea y barras.
 */
public final class ConfiguracionGeminiDtos {

    public static final String PATRON_MODELO = "^[A-Za-z0-9._-]{1,80}$";
    /** Caracteres ASCII visibles, sin espacios (evita inyectar cabeceras). */
    public static final String PATRON_CLAVE = "^[\\x21-\\x7E]*$";

    private ConfiguracionGeminiDtos() {
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record Respuesta(OrigenConfiguracionGemini origenClave, String claveEnmascarada,
                            boolean claveGuardadaIlegible, OrigenConfiguracionGemini origenModelos,
                            List<EstadoModelo> modelos, Long actualizadoPor, Instant actualizadoEn) {
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record EstadoModelo(String modelo, boolean disponible, Instant apartadoHasta, String motivo) {
    }

    public record Actualizar(
            @Size(max = 200, message = "La clave no puede superar 200 caracteres")
            @Pattern(regexp = PATRON_CLAVE, message = "La clave no puede contener espacios")
            String apiKey,

            boolean eliminarClave,

            @NotEmpty(message = "Indica al menos un modelo de Gemini")
            @Size(max = 15, message = "No se pueden configurar mas de 15 modelos")
            List<@NotBlank @Pattern(regexp = PATRON_MODELO, message = "Nombre de modelo invalido") String> modelos
    ) {
    }

    public record Probar(
            @Size(max = 200, message = "La clave no puede superar 200 caracteres")
            @Pattern(regexp = PATRON_CLAVE, message = "La clave no puede contener espacios")
            String apiKey,

            @NotBlank(message = "Indica el modelo a probar")
            @Pattern(regexp = PATRON_MODELO, message = "Nombre de modelo invalido")
            String modelo
    ) {
    }

    public record ListarModelos(
            @Size(max = 200, message = "La clave no puede superar 200 caracteres")
            @Pattern(regexp = PATRON_CLAVE, message = "La clave no puede contener espacios")
            String apiKey
    ) {
    }

    public record ModeloDisponible(String id, String nombre, String descripcion) {
    }

    public record ResultadoPrueba(boolean exitoso, String modelo, long milisegundos, String mensaje) {
    }
}

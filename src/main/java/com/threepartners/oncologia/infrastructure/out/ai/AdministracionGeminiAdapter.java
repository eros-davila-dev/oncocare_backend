package com.threepartners.oncologia.infrastructure.out.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.threepartners.oncologia.domain.chatbot.AdministracionGeminiPort;
import com.threepartners.oncologia.domain.chatbot.ConfiguracionGeminiVigente;
import com.threepartners.oncologia.domain.chatbot.EstadoModeloGemini;
import com.threepartners.oncologia.domain.chatbot.ModeloGeminiDisponible;
import com.threepartners.oncologia.domain.chatbot.ResultadoPruebaGemini;
import com.threepartners.oncologia.domain.shared.exception.ValidacionDeNegocioException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Lado administrativo de la integracion con Gemini: consulta a Google que
 * modelos puede usar una clave y la prueba con un mensaje minimo, para que
 * el administrador sepa si funciona antes de dejar al chatbot sin servicio.
 */
@Slf4j
@Component
public class AdministracionGeminiAdapter implements AdministracionGeminiPort {

    private final GeminiProperties propiedades;
    private final ResolutorConfiguracionGemini resolutor;
    private final RotacionModelosGemini rotacion;
    private final ObjectMapper objectMapper;
    private final RestClient cliente;

    public AdministracionGeminiAdapter(GeminiProperties propiedades, ResolutorConfiguracionGemini resolutor,
                                       RotacionModelosGemini rotacion, ObjectMapper objectMapper,
                                       RestClient.Builder restClientBuilder) {
        this.propiedades = propiedades;
        this.resolutor = resolutor;
        this.rotacion = rotacion;
        this.objectMapper = objectMapper;
        var fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofSeconds(5));
        fabrica.setReadTimeout(Duration.ofSeconds(20));
        this.cliente = restClientBuilder.clone().requestFactory(fabrica).build();
    }

    @Override
    public ConfiguracionGeminiVigente vigente() {
        return resolutor.vigente();
    }

    @Override
    public List<ModeloGeminiDisponible> listarModelos(String apiKey) {
        String clave = claveOVigente(apiKey);
        try {
            String cuerpo = cliente.get()
                    .uri("%s/v1beta/models?pageSize=1000".formatted(propiedades.baseUrl()))
                    .header("x-goog-api-key", clave)
                    .retrieve()
                    .body(String.class);
            List<ModeloGeminiDisponible> modelos = new ArrayList<>();
            for (JsonNode m : objectMapper.readTree(cuerpo).path("models")) {
                String id = m.path("name").asText("").replaceFirst("^models/", "");
                if (!id.startsWith("gemini") || !admiteGenerarContenido(m)) {
                    continue;
                }
                modelos.add(new ModeloGeminiDisponible(id, m.path("displayName").asText(id),
                        m.path("description").asText(null)));
            }
            modelos.sort(Comparator.comparing(ModeloGeminiDisponible::id).reversed());
            return modelos;
        } catch (HttpClientErrorException e) {
            throw new ValidacionDeNegocioException(mensajeError(e.getStatusCode().value(), e.getResponseBodyAsString(), null));
        } catch (RestClientException | java.io.IOException e) {
            log.warn("Gemini: no se pudo consultar la lista de modelos ({})", e.getClass().getSimpleName());
            throw new ValidacionDeNegocioException("No se pudo contactar a Google para listar los modelos. Intenta de nuevo.");
        }
    }

    @Override
    public ResultadoPruebaGemini probar(String apiKey, String modelo) {
        String clave = claveOVigente(apiKey);
        ObjectNode cuerpo = objectMapper.createObjectNode();
        cuerpo.putArray("contents").addObject()
                .put("role", "user")
                .putArray("parts").addObject().put("text", "Responde solo con la palabra: ok");
        long inicio = System.nanoTime();
        try {
            cliente.post()
                    .uri("%s/v1beta/models/%s:generateContent".formatted(propiedades.baseUrl(), modelo))
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("x-goog-api-key", clave)
                    .body(cuerpo)
                    .retrieve()
                    .body(String.class);
            return new ResultadoPruebaGemini(true, modelo, milisDesde(inicio), "El modelo respondio correctamente.");
        } catch (HttpClientErrorException e) {
            return new ResultadoPruebaGemini(false, modelo, milisDesde(inicio),
                    mensajeError(e.getStatusCode().value(), e.getResponseBodyAsString(), modelo));
        } catch (HttpServerErrorException e) {
            return new ResultadoPruebaGemini(false, modelo, milisDesde(inicio),
                    "Google esta saturado o con fallas (HTTP " + e.getStatusCode().value() + "). Intenta en unos minutos.");
        } catch (RestClientException e) {
            return new ResultadoPruebaGemini(false, modelo, milisDesde(inicio),
                    "Google no respondio a tiempo. Revisa la conexion a internet del servidor.");
        }
    }

    @Override
    public List<EstadoModeloGemini> estado(List<String> modelos) {
        return rotacion.estado(modelos);
    }

    @Override
    public void reiniciarRotacion() {
        rotacion.reiniciar();
    }

    private String claveOVigente(String apiKey) {
        if (apiKey != null && !apiKey.isBlank()) {
            return apiKey.strip();
        }
        ConfiguracionGeminiVigente vigente = resolutor.vigente();
        if (!vigente.tieneClave()) {
            throw new ValidacionDeNegocioException("No hay una clave de API de Gemini configurada. Ingresa una para continuar.");
        }
        return vigente.apiKey();
    }

    private static boolean admiteGenerarContenido(JsonNode modelo) {
        for (JsonNode metodo : modelo.path("supportedGenerationMethods")) {
            if ("generateContent".equals(metodo.asText())) {
                return true;
            }
        }
        return false;
    }

    /** Traduce los errores de Google a algo que el administrador pueda resolver. Nunca incluye la clave. */
    private String mensajeError(int estado, String cuerpo, String modelo) {
        String detalle = "";
        try {
            detalle = objectMapper.readTree(cuerpo == null ? "{}" : cuerpo).path("error").path("message").asText("");
        } catch (Exception ignorada) {
            // cuerpo no JSON: se responde solo con el codigo
        }
        boolean claveInvalida = detalle.contains("API key not valid") || detalle.contains("API_KEY_INVALID");
        return switch (estado) {
            case 400 -> claveInvalida ? "La clave de API no es valida." : "Google rechazo la solicitud: " + detalle;
            case 401, 403 -> claveInvalida ? "La clave de API no es valida."
                    : "La clave no tiene permiso para usar la API de Gemini" + (modelo != null ? " con " + modelo : "") + ".";
            case 404 -> "El modelo " + (modelo != null ? modelo + " " : "") + "no existe o no esta disponible para esta clave.";
            case 429 -> "La clave es valida, pero el modelo " + (modelo != null ? modelo + " " : "")
                    + "agoto su cuota por ahora. El chatbot usara el siguiente modelo de la lista.";
            default -> "Google respondio HTTP " + estado + (detalle.isBlank() ? "" : ": " + detalle);
        };
    }

    private static long milisDesde(long inicioNanos) {
        return (System.nanoTime() - inicioNanos) / 1_000_000;
    }
}

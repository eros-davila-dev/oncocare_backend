package com.threepartners.oncologia.infrastructure.out.correo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * API HTTP de correo (servicemail / Quipu), el proveedor principal. Contrato
 * igual al que ya usa la fundacion en sus otros sistemas: POST a MAIL_API_URL
 * con la cabecera X-API-Token y el cuerpo {from, fromName, to, subject,
 * body_html}. Una respuesta 2xx es exito, salvo que el cuerpo traiga un
 * "status" distinto de "success".
 */
public class ApiCorreoProveedor implements ProveedorCorreo {

    private static final int TIMEOUT_CONEXION_MILIS = 5_000;
    private static final int TIMEOUT_LECTURA_MILIS = 15_000;

    private final String url;
    private final String clave;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public ApiCorreoProveedor(String url, String clave, ObjectMapper objectMapper) {
        if (url == null || !(url.startsWith("http://") || url.startsWith("https://"))) {
            throw new IllegalStateException("MAIL_FLAG_QUIPU=true requiere MAIL_API_URL con una URL http(s) absoluta");
        }
        if (clave == null || clave.isBlank()) {
            throw new IllegalStateException("MAIL_FLAG_QUIPU=true requiere MAIL_API_KEY");
        }
        this.url = url;
        this.clave = clave;
        this.objectMapper = objectMapper;
        var fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(TIMEOUT_CONEXION_MILIS);
        fabrica.setReadTimeout(TIMEOUT_LECTURA_MILIS);
        this.restClient = RestClient.builder().requestFactory(fabrica).build();
    }

    @Override
    public String nombre() {
        return "api";
    }

    @Override
    public void enviar(MensajeCorreo mensaje, String remitente, String nombreRemitente) {
        String respuesta = restClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-API-Token", clave)
                .body(new Solicitud(remitente, nombreRemitente, List.of(mensaje.destinatario()),
                        mensaje.asunto(), mensaje.html()))
                .retrieve()
                .body(String.class);
        verificarConfirmacion(respuesta);
    }

    private void verificarConfirmacion(String respuesta) {
        if (respuesta == null || respuesta.isBlank()) {
            return;
        }
        try {
            JsonNode cuerpo = objectMapper.readTree(respuesta);
            JsonNode estado = cuerpo.get("status");
            if (estado != null && !"success".equalsIgnoreCase(estado.asText())) {
                JsonNode detalle = cuerpo.get("message");
                throw new IllegalStateException("La API de correo no confirmo el envio: "
                        + (detalle != null ? detalle.asText() : estado.asText()));
            }
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            // Respuesta 2xx que no es JSON: se considera aceptada.
        }
    }

    record Solicitud(String from, String fromName, List<String> to, String subject,
                     @JsonProperty("body_html") String bodyHtml) {
    }
}

package com.threepartners.oncologia.infrastructure.out.notification;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Cliente HTTP saliente hacia los webhooks de n8n, usado solo por el relevo
 * del outbox a traves de EntregaExternaEnrutador: los casos de uso nunca lo
 * llaman directamente.
 *
 * Usa su propio RestClient con timeouts cortos (no el RestClient.Builder
 * global usado para Gemini, que necesita mas margen para una respuesta de
 * IA). Si n8n no responde o devuelve un error, lanza la excepcion: el outbox
 * la registra y reintenta mas tarde.
 */
@Component
public class N8nNotificationAdapter {

    private static final int TIMEOUT_CONEXION_MILIS = 3_000;
    private static final int TIMEOUT_LECTURA_MILIS = 5_000;

    private final N8nProperties n8nProperties;
    private final RestClient restClient;

    public N8nNotificationAdapter(N8nProperties n8nProperties) {
        this.n8nProperties = n8nProperties;
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(TIMEOUT_CONEXION_MILIS);
        requestFactory.setReadTimeout(TIMEOUT_LECTURA_MILIS);
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    public void entregar(String destino, Map<String, Object> payload) {
        restClient.post()
                .uri(n8nProperties.baseUrl() + destino)
                .header("X-Webhook-Secret", n8nProperties.webhookSecret())
                .body(payload)
                .retrieve()
                .toBodilessEntity();
    }
}

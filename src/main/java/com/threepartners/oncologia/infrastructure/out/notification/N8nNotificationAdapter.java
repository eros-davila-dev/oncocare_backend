package com.threepartners.oncologia.infrastructure.out.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Cliente HTTP saliente hacia n8n (seccion 6). Los tres flujos de la seccion
 * 15 son iniciados por n8n hacia el backend, pero este adaptador queda
 * disponible para disparar un workflow puntual de forma inmediata (por
 * ejemplo, notificar una cancelacion de cita sin esperar al cron diario) sin
 * que los casos de uso de negocio conozcan como esta implementado n8n.
 *
 * Es deliberadamente "dispara y olvida": usa su propio RestClient con
 * timeouts cortos (no el RestClient.Builder global usado para Gemini, que
 * necesita mas margen para una respuesta de IA) para que un n8n lento, caido
 * o sin el workflow importado todavia nunca retrase la respuesta al usuario
 * ni mantenga abierta una transaccion de base de datos.
 */
@Slf4j
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

    public void dispararWorkflow(String rutaWebhook, Map<String, Object> payload) {
        try {
            restClient.post()
                    .uri(n8nProperties.baseUrl() + rutaWebhook)
                    .header("X-Webhook-Secret", n8nProperties.webhookSecret())
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.error("No se pudo notificar al workflow de n8n en {}", rutaWebhook, e);
        }
    }
}

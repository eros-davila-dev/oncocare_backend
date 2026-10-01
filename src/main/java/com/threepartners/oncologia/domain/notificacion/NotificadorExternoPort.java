package com.threepartners.oncologia.domain.notificacion;

import java.util.Map;

/**
 * Puerto de salida hacia el orquestador de mensajeria (hoy n8n). Los casos de
 * uso solo saben que "disparan un flujo" con un payload; no conocen la URL,
 * el secreto ni la tecnologia, de modo que cambiar n8n por otra herramienta
 * solo afecta al adaptador.
 */
public interface NotificadorExternoPort {

    void dispararWorkflow(String rutaWebhook, Map<String, Object> payload);
}

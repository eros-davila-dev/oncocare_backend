package com.threepartners.oncologia.domain.notificacion;

import java.util.Map;

/**
 * Puerto de salida hacia el orquestador de mensajeria (hoy n8n). Los casos de
 * uso solo saben que "disparan un flujo" con un payload; no conocen la URL,
 * el secreto ni la tecnologia, de modo que cambiar n8n por otra herramienta
 * solo afecta al adaptador.
 *
 * El aviso se encola en el outbox dentro de la transaccion del llamador y se
 * entrega despues, con reintentos: llamar a este metodo nunca hace una
 * peticion HTTP ni puede fallar por culpa de n8n.
 */
public interface NotificadorExternoPort {

    void dispararWorkflow(String rutaWebhook, Map<String, Object> payload);
}

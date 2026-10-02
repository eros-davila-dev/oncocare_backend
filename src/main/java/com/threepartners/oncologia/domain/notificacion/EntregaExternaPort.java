package com.threepartners.oncologia.domain.notificacion;

import java.util.Map;

/**
 * Entrega efectiva de un aviso al orquestador (hoy, un webhook de n8n). A
 * diferencia de {@link NotificadorExternoPort}, falla con una excepcion para
 * que el outbox pueda reintentar.
 */
public interface EntregaExternaPort {

    void entregar(String destino, Map<String, Object> payload);
}

package com.threepartners.oncologia.application.notificacion;

import com.threepartners.oncologia.domain.notificacion.EventoSalienteRepositoryPort;
import com.threepartners.oncologia.domain.notificacion.NotificadorExternoPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Map;

/**
 * Los casos de uso "disparan un workflow" como siempre, pero ahora el aviso
 * se guarda en el outbox en su misma transaccion: se envia solo si lo que lo
 * origino quedo guardado, y sobrevive a un n8n caido.
 */
@Service
@RequiredArgsConstructor
public class EncolarNotificacionExterna implements NotificadorExternoPort {

    private final EventoSalienteRepositoryPort eventoSalienteRepositoryPort;
    private final Clock clock;

    @Override
    @Transactional
    public void dispararWorkflow(String rutaWebhook, Map<String, Object> payload) {
        eventoSalienteRepositoryPort.encolar(rutaWebhook, payload, clock.instant());
    }
}

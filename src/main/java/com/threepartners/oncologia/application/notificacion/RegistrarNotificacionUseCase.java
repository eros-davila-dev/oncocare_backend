package com.threepartners.oncologia.application.notificacion;

import com.threepartners.oncologia.domain.notificacion.Notificacion;
import com.threepartners.oncologia.domain.notificacion.NotificacionRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Invocado por n8n (POST /api/notificaciones) tras enviar un recordatorio de
 * cita por WhatsApp o correo, para dejar registro del resultado del envio
 * (seccion 15, flujo 2).
 */
@Service
@RequiredArgsConstructor
public class RegistrarNotificacionUseCase {

    private final NotificacionRepositoryPort notificacionRepositoryPort;

    @Transactional
    public Notificacion ejecutar(Notificacion notificacion) {
        if (notificacion.getFechaEnvio() == null) {
            notificacion.setFechaEnvio(Instant.now());
        }
        return notificacionRepositoryPort.guardar(notificacion);
    }
}

package com.threepartners.oncologia.application.notificacion;

import com.threepartners.oncologia.domain.notificacion.EventoSalienteRepositoryPort;
import com.threepartners.oncologia.domain.notificacion.NotificadorCorreoPort;
import com.threepartners.oncologia.domain.notificacion.TipoCorreo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Map;

/**
 * Pone el correo en el outbox con destino "correo:TIPO". El relevo
 * (EntregarEventosSalientesUseCase) lo entrega al proveedor de correo con
 * reintentos y vacia el payload al enviarlo: el correo y el enlace con token
 * no quedan guardados.
 */
@Service
@RequiredArgsConstructor
public class EncolarCorreo implements NotificadorCorreoPort {

    public static final String PREFIJO_DESTINO = "correo:";

    private final EventoSalienteRepositoryPort eventoSalienteRepositoryPort;
    private final Clock clock;

    @Override
    @Transactional
    public void enviar(TipoCorreo tipo, String email, String nombre, String enlace) {
        eventoSalienteRepositoryPort.encolar(PREFIJO_DESTINO + tipo.name(),
                Map.of("email", email, "nombre", nombre != null ? nombre : "", "enlace", enlace),
                clock.instant());
    }
}

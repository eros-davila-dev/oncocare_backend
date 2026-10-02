package com.threepartners.oncologia.application.notificacion;

import com.threepartners.oncologia.domain.notificacion.EntregaExternaPort;
import com.threepartners.oncologia.domain.notificacion.EventoSaliente;
import com.threepartners.oncologia.domain.notificacion.EventoSalienteRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Relevo del outbox: reclama un lote, lo entrega y registra el resultado de
 * cada aviso. Deliberadamente sin transaccion propia: la peticion HTTP nunca
 * ocurre con una transaccion abierta ni con filas bloqueadas (el reclamo es
 * una sola sentencia atomica que deja los avisos EN_ENVIO).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EntregarEventosSalientesUseCase {

    static final int LOTE = 50;
    /** Un aviso EN_ENVIO mas tiempo que esto pertenecia a una instancia caida. */
    static final Duration COLGADO_TRAS = Duration.ofMinutes(5);
    static final Duration RETENCION_ENVIADOS = Duration.ofDays(30);

    private final EventoSalienteRepositoryPort eventoSalienteRepositoryPort;
    private final EntregaExternaPort entregaExternaPort;
    private final Clock clock;

    public ResultadoEntrega ejecutar() {
        Instant ahora = clock.instant();
        int liberados = eventoSalienteRepositoryPort.liberarColgados(ahora.minus(COLGADO_TRAS));
        if (liberados > 0) {
            log.warn("Outbox: {} avisos quedaron en envio sin respuesta y vuelven a la cola", liberados);
        }

        int enviados = 0;
        int reintentos = 0;
        int fallidos = 0;
        for (EventoSaliente evento : eventoSalienteRepositoryPort.reclamarPendientes(LOTE, ahora)) {
            try {
                entregaExternaPort.entregar(evento.destino(), evento.payload());
                eventoSalienteRepositoryPort.marcarEnviado(evento.id(), clock.instant());
                enviados++;
            } catch (RuntimeException e) {
                String error = resumen(e);
                if (evento.agotoReintentos()) {
                    eventoSalienteRepositoryPort.marcarFallido(evento.id(), evento.intentosTrasFallo(), error);
                    // Sin el payload en el log: puede llevar un correo o un enlace con token.
                    log.error("Outbox: el aviso {} a {} fallo {} veces y se descarta: {}",
                            evento.id(), evento.destino(), evento.intentosTrasFallo(), error);
                    fallidos++;
                } else {
                    eventoSalienteRepositoryPort.reprogramar(evento.id(), evento.intentosTrasFallo(),
                            clock.instant().plus(evento.esperaAntesDelSiguienteIntento()), error);
                    log.warn("Outbox: no se pudo entregar el aviso {} a {} (intento {}): {}",
                            evento.id(), evento.destino(), evento.intentosTrasFallo(), error);
                    reintentos++;
                }
            }
        }
        int purgados = eventoSalienteRepositoryPort.purgarEnviados(ahora.minus(RETENCION_ENVIADOS));
        return new ResultadoEntrega(enviados, reintentos, fallidos, purgados);
    }

    private static String resumen(RuntimeException e) {
        String mensaje = e.getClass().getSimpleName() + ": " + e.getMessage();
        return mensaje.length() > 500 ? mensaje.substring(0, 500) : mensaje;
    }

    public record ResultadoEntrega(int enviados, int reintentos, int fallidos, int purgados) {
    }
}

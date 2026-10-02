package com.threepartners.oncologia.infrastructure.in.scheduler;

import com.threepartners.oncologia.application.notificacion.EntregarEventosSalientesUseCase;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Cada pocos segundos entrega a n8n los avisos del outbox (consultas
 * escaladas, correos). Un aviso al personal llega en segundos, no en el
 * siguiente cron.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.jobs.habilitados", havingValue = "true", matchIfMissing = true)
public class EventosSalientesJob {

    private final EntregarEventosSalientesUseCase entregarEventosSalientesUseCase;
    private final MeterRegistry meterRegistry;

    @Scheduled(fixedDelayString = "${app.outbox.intervalo:PT5S}", initialDelayString = "PT15S")
    @SchedulerLock(name = "entregar-eventos-salientes", lockAtMostFor = "PT2M")
    public void ejecutar() {
        var resultado = entregarEventosSalientesUseCase.ejecutar();
        contar("enviado", resultado.enviados());
        contar("reintento", resultado.reintentos());
        contar("fallido", resultado.fallidos());
    }

    private void contar(String resultado, int cantidad) {
        if (cantidad > 0) {
            meterRegistry.counter("oncologia.outbox.entregas", "resultado", resultado).increment(cantidad);
        }
    }
}

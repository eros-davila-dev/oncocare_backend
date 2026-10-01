package com.threepartners.oncologia.infrastructure.in.scheduler;

import com.threepartners.oncologia.application.cita.CerrarCitasSinDesenlaceUseCase;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Cada hora cierra las citas que llevan mas del plazo configurado sin
 * desenlace (indicador TNS).
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.jobs.habilitados", havingValue = "true", matchIfMissing = true)
public class CitasSinDesenlaceJob {

    private final CerrarCitasSinDesenlaceUseCase cerrarCitasSinDesenlaceUseCase;

    @Scheduled(cron = "0 5 * * * *", zone = "America/Lima")
    @SchedulerLock(name = "citas-sin-desenlace", lockAtMostFor = "PT10M")
    public void ejecutar() {
        cerrarCitasSinDesenlaceUseCase.ejecutar();
    }
}

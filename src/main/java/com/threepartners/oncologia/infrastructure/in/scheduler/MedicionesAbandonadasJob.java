package com.threepartners.oncologia.infrastructure.in.scheduler;

import com.threepartners.oncologia.application.estudio.MarcarMedicionesAbandonadasUseCase;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Adaptador de entrada "por tiempo": cada 15 minutos cierra como ABANDONADA
 * toda sesion de medicion del TPR que nunca se guardo.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.jobs.habilitados", havingValue = "true", matchIfMissing = true)
public class MedicionesAbandonadasJob {

    private final MarcarMedicionesAbandonadasUseCase marcarMedicionesAbandonadasUseCase;

    @Scheduled(cron = "0 */15 * * * *", zone = "America/Lima")
    @SchedulerLock(name = "mediciones-abandonadas", lockAtMostFor = "PT5M")
    public void ejecutar() {
        marcarMedicionesAbandonadasUseCase.ejecutar();
    }
}

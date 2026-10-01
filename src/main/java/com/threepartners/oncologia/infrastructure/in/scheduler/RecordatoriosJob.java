package com.threepartners.oncologia.infrastructure.in.scheduler;

import com.threepartners.oncologia.application.recordatorio.ProgramarRecordatoriosUseCase;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Cada 15 minutos programa los recordatorios de las citas proximas (el envio
 * lo hace n8n consultando los pendientes).
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.jobs.habilitados", havingValue = "true", matchIfMissing = true)
public class RecordatoriosJob {

    private final ProgramarRecordatoriosUseCase programarRecordatoriosUseCase;

    @Scheduled(cron = "${app.recordatorios.cron:0 2/15 * * * *}", zone = "America/Lima")
    @SchedulerLock(name = "programar-recordatorios", lockAtMostFor = "PT10M")
    public void ejecutar() {
        programarRecordatoriosUseCase.ejecutar();
    }
}

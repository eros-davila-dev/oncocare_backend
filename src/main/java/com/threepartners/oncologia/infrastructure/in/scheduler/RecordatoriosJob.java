package com.threepartners.oncologia.infrastructure.in.scheduler;

import com.threepartners.oncologia.application.recordatorio.EnviarRecordatoriosCorreoUseCase;
import com.threepartners.oncologia.application.recordatorio.ProgramarRecordatoriosUseCase;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Cada 15 minutos programa los recordatorios de las citas proximas. Los de
 * Telegram los envia n8n consultando los pendientes; los de correo, el
 * propio backend cada 5 minutos.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.jobs.habilitados", havingValue = "true", matchIfMissing = true)
public class RecordatoriosJob {

    private final ProgramarRecordatoriosUseCase programarRecordatoriosUseCase;
    private final EnviarRecordatoriosCorreoUseCase enviarRecordatoriosCorreoUseCase;

    @Scheduled(cron = "${app.recordatorios.cron:0 2/15 * * * *}", zone = "America/Lima")
    @SchedulerLock(name = "programar-recordatorios", lockAtMostFor = "PT10M")
    public void ejecutar() {
        programarRecordatoriosUseCase.ejecutar();
    }

    @Scheduled(cron = "${app.recordatorios.cron-correo:0 1/5 * * * *}", zone = "America/Lima")
    @SchedulerLock(name = "enviar-recordatorios-correo", lockAtMostFor = "PT4M")
    public void enviarCorreos() {
        enviarRecordatoriosCorreoUseCase.ejecutar();
    }
}

package com.threepartners.oncologia.infrastructure.in.scheduler;

import com.threepartners.oncologia.application.chatbot.CerrarConsultasVencidasUseCase;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Cada 10 minutos cierra las consultas abandonadas o sin atencion del
 * personal (indicador NCA).
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.jobs.habilitados", havingValue = "true", matchIfMissing = true)
public class ConsultasVencidasJob {

    private final CerrarConsultasVencidasUseCase cerrarConsultasVencidasUseCase;

    @Scheduled(cron = "0 */10 * * * *", zone = "America/Lima")
    @SchedulerLock(name = "consultas-vencidas", lockAtMostFor = "PT5M")
    public void ejecutar() {
        cerrarConsultasVencidasUseCase.ejecutar();
    }
}

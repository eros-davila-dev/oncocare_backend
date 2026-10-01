package com.threepartners.oncologia.application.cita;

import com.threepartners.oncologia.config.AgendaProperties;
import com.threepartners.oncologia.domain.auditoria.event.OperacionAuditadaEvent;
import com.threepartners.oncologia.domain.cita.Cita;
import com.threepartners.oncologia.domain.cita.CitaRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Invocado por un job: una cita que paso hace mas de N horas (48 por defecto)
 * sin que nadie registrara su desenlace se cierra como NO_ASISTIO con
 * cierre_automatico = true. Sin esto, las citas olvidadas quedarian fuera del
 * denominador del TNS y lo sesgarian. El personal puede corregirlo despues
 * con motivo (corregir-desenlace).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CerrarCitasSinDesenlaceUseCase {

    private final CitaRepositoryPort citaRepositoryPort;
    private final AgendaProperties agendaProperties;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @Transactional
    public int ejecutar() {
        LocalDateTime limite = LocalDateTime.now(clock).minusHours(agendaProperties.horasCierreAutomatico());
        Instant ahora = clock.instant();
        var pendientes = citaRepositoryPort.sinDesenlaceAntesDe(limite);
        for (Cita cita : pendientes) {
            String previo = "estado=" + cita.getEstado();
            cita.marcarNoAsistio(null, ahora, true);
            citaRepositoryPort.guardar(cita);
            eventPublisher.publishEvent(OperacionAuditadaEvent.exito(null, "CITA_CIERRE_AUTOMATICO", "CITA",
                    cita.getId(), previo, "estado=NO_ASISTIO;cierreAutomatico=true", "sistema"));
        }
        if (!pendientes.isEmpty()) {
            log.info("{} citas sin desenlace cerradas automaticamente como NO_ASISTIO", pendientes.size());
        }
        return pendientes.size();
    }
}

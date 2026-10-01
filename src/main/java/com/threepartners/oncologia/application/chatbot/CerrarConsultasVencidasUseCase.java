package com.threepartners.oncologia.application.chatbot;

import com.threepartners.oncologia.domain.estudio.Consulta;
import com.threepartners.oncologia.domain.estudio.ConsultaRepositoryPort;
import com.threepartners.oncologia.domain.estudio.ResultadoConsulta;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Invocado por un job. Sin esto, las consultas que nadie cierra quedarian
 * fuera del denominador del NCA y lo inflarian:
 * - abiertas sin actividad por 30 minutos: el usuario abandono sin que se
 *   resolviera su necesidad (NO_RESUELTA);
 * - escaladas sin atencion del personal por 48 horas (NO_RESUELTA).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CerrarConsultasVencidasUseCase {

    static final Duration INACTIVIDAD_MAXIMA = Duration.ofMinutes(30);
    static final Duration ESPERA_MAXIMA_PERSONAL = Duration.ofHours(48);

    private final ConsultaRepositoryPort consultaRepositoryPort;
    private final Clock clock;

    @Transactional
    public int ejecutar() {
        Instant ahora = clock.instant();
        int cerradas = 0;
        for (Consulta c : consultaRepositoryPort.abiertasSinActividadAntesDe(ahora.minus(INACTIVIDAD_MAXIMA))) {
            c.cerrar(ResultadoConsulta.NO_RESUELTA, null, ahora);
            consultaRepositoryPort.guardar(c);
            cerradas++;
        }
        for (Consulta c : consultaRepositoryPort.escaladasSinActividadAntesDe(ahora.minus(ESPERA_MAXIMA_PERSONAL))) {
            c.setNotaResolucion("Cerrada automaticamente: sin atencion del personal en 48 horas");
            c.cerrar(ResultadoConsulta.NO_RESUELTA, null, ahora);
            consultaRepositoryPort.guardar(c);
            cerradas++;
        }
        if (cerradas > 0) {
            log.info("{} consultas vencidas cerradas como NO_RESUELTA", cerradas);
        }
        return cerradas;
    }
}

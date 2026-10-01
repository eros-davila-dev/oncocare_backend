package com.threepartners.oncologia.application.estudio;

import com.threepartners.oncologia.config.EstudioProperties;
import com.threepartners.oncologia.domain.estudio.MedicionRegistroRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;

/**
 * Invocado por un job: una sesion de medicion que lleva abierta mas del
 * maximo configurado corresponde a un formulario que nunca se guardo. Se marca
 * ABANDONADA para que no quede "en curso" para siempre ni entre al TPR.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MarcarMedicionesAbandonadasUseCase {

    private final MedicionRegistroRepositoryPort medicionRegistroRepositoryPort;
    private final EstudioProperties estudioProperties;
    private final Clock clock;

    @Transactional
    public int ejecutar() {
        var limite = clock.instant().minus(Duration.ofMinutes(estudioProperties.minutosMaximosRegistro()));
        int marcadas = medicionRegistroRepositoryPort.marcarAbandonadasIniciadasAntesDe(limite);
        if (marcadas > 0) {
            log.info("{} sesiones de medicion de registro marcadas como ABANDONADA", marcadas);
        }
        return marcadas;
    }
}

package com.threepartners.oncologia.config;

import com.threepartners.oncologia.domain.shared.ZonaHoraria;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Reloj unico del sistema. Los casos de uso que sellan o comparan tiempos
 * (mediciones del TPR, cierre de citas, recordatorios) lo reciben inyectado en
 * vez de llamar a Instant.now(), para que los tests puedan fijar el tiempo
 * con Clock.fixed y verificar los indicadores de forma determinista.
 */
@Configuration
public class TiempoConfig {

    @Bean
    public Clock clock() {
        return Clock.system(ZonaHoraria.LIMA);
    }
}

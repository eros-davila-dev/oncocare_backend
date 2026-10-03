package com.threepartners.oncologia.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.transaction.TransactionAwareCacheManagerProxy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Cache local (Caffeine) solo para catalogos que se leen mucho y cambian
 * poco. Dos decisiones:
 *
 * - Transaccional: una invalidacion se aplica despues del commit; si no, una
 *   lectura concurrente podria volver a cachear el dato viejo.
 * - Vencimiento de 5 min: con varias instancias la invalidacion es local,
 *   asi que el vencimiento acota cuanto puede tardar otra instancia en ver
 *   el cambio.
 *
 * Los indicadores de la tesis NO se cachean: el volumen del estudio es
 * pequeno, sus consultas son agregados sobre indices, y el investigador
 * necesita ver el dato recien capturado o anulado.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String PREGUNTAS_FRECUENTES_ACTIVAS = "preguntas-frecuentes-activas";
    public static final String CONFIGURACION_GEMINI = "configuracion-gemini";

    @Bean
    CacheManager cacheManager() {
        var caffeine = new CaffeineCacheManager(PREGUNTAS_FRECUENTES_ACTIVAS, CONFIGURACION_GEMINI);
        caffeine.setCaffeine(Caffeine.newBuilder().expireAfterWrite(Duration.ofMinutes(5)).maximumSize(100).recordStats());
        caffeine.setAllowNullValues(false);
        return new TransactionAwareCacheManagerProxy(caffeine);
    }
}

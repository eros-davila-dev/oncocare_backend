package com.threepartners.oncologia.config.ratelimit;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Clock;

/**
 * Elige donde se cuentan las solicitudes: app.rate-limit.almacen=memoria
 * (por defecto) o redis (produccion, varias instancias).
 */
@Configuration
public class RateLimitConfig {

    @Bean
    @ConditionalOnProperty(name = "app.rate-limit.almacen", havingValue = "redis")
    ContadorSolicitudes contadorSolicitudesRedis(ObjectProvider<StringRedisTemplate> redis, Clock clock) {
        return new ContadorSolicitudesRedis(redis.getObject(), new ContadorSolicitudesEnMemoria(clock));
    }

    @Bean
    @ConditionalOnProperty(name = "app.rate-limit.almacen", havingValue = "memoria", matchIfMissing = true)
    ContadorSolicitudes contadorSolicitudesEnMemoria(Clock clock) {
        return new ContadorSolicitudesEnMemoria(clock);
    }
}

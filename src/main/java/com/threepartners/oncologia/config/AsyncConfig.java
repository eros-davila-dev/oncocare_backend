package com.threepartners.oncologia.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Habilita @Async para el listener de auditoria (seccion 13): un fallo o una
 * demora en registrar la auditoria nunca debe bloquear ni revertir la
 * operacion de negocio principal.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "auditoriaTaskExecutor")
    public Executor auditoriaTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("auditoria-");
        executor.initialize();
        return executor;
    }
}

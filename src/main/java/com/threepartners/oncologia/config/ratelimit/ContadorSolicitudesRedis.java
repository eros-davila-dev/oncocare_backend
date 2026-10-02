package com.threepartners.oncologia.config.ratelimit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.List;

/**
 * Ventana fija compartida por todas las instancias del backend. INCR y
 * PEXPIRE van en un solo script para que una clave nunca quede sin
 * vencimiento (lo que bloquearia una IP para siempre).
 *
 * Si Redis no responde, cuenta en memoria en vez de rechazar o dejar pasar
 * todo: el limite sigue funcionando por instancia mientras Redis vuelve.
 */
@Slf4j
public class ContadorSolicitudesRedis implements ContadorSolicitudes {

    static final String PREFIJO = "oncologia:rl:";

    static final RedisScript<Long> INCREMENTAR = RedisScript.of("""
            local n = redis.call('INCR', KEYS[1])
            if n == 1 then redis.call('PEXPIRE', KEYS[1], ARGV[1]) end
            return n
            """, Long.class);

    private final StringRedisTemplate redis;
    private final ContadorSolicitudes respaldo;

    public ContadorSolicitudesRedis(StringRedisTemplate redis, ContadorSolicitudes respaldo) {
        this.redis = redis;
        this.respaldo = respaldo;
    }

    @Override
    public long incrementar(String clave, Duration ventana) {
        try {
            Long n = redis.execute(INCREMENTAR, List.of(PREFIJO + clave), String.valueOf(ventana.toMillis()));
            if (n != null) {
                return n;
            }
        } catch (RuntimeException e) {
            log.warn("Rate limit: Redis no disponible, se cuenta en memoria ({})", e.getClass().getSimpleName());
        }
        return respaldo.incrementar(clave, ventana);
    }
}

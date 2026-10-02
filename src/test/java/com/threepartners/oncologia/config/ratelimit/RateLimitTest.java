package com.threepartners.oncologia.config.ratelimit;

import com.threepartners.oncologia.config.RateLimitFilter;
import com.threepartners.oncologia.domain.shared.ZonaHoraria;
import com.threepartners.oncologia.soporte.RelojDePrueba;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RateLimitTest {

    private static final Duration MINUTO = Duration.ofMinutes(1);

    private final RelojDePrueba reloj = new RelojDePrueba(Instant.parse("2026-10-05T15:00:00Z"), ZonaHoraria.LIMA);

    @Test
    void enMemoriaCuentaPorClaveYReiniciaAlVencerLaVentana() {
        var contador = new ContadorSolicitudesEnMemoria(reloj);

        assertThat(contador.incrementar("login|1.1.1.1", MINUTO)).isEqualTo(1);
        assertThat(contador.incrementar("login|1.1.1.1", MINUTO)).isEqualTo(2);
        assertThat(contador.incrementar("login|2.2.2.2", MINUTO)).isEqualTo(1);

        reloj.avanzar(Duration.ofSeconds(61));
        assertThat(contador.incrementar("login|1.1.1.1", MINUTO)).isEqualTo(1);
    }

    @Test
    void enMemoriaDescartaLasVentanasVencidasParaNoCrecerSinLimite() {
        var contador = new ContadorSolicitudesEnMemoria(reloj);
        for (int i = 0; i < 500; i++) {
            contador.incrementar("ip-" + i, MINUTO);
        }
        reloj.avanzar(Duration.ofMinutes(2));
        for (int i = 0; i < 500; i++) {
            contador.incrementar("otra", MINUTO);
        }

        assertThat(contador.ventanasActivas()).isEqualTo(1);
    }

    @Test
    void enRedisUsaElScriptAtomicoConLaVentanaEnMilisegundos() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.execute(eq(ContadorSolicitudesRedis.INCREMENTAR), anyList(), any(Object[].class))).thenReturn(7L);

        long n = new ContadorSolicitudesRedis(redis, new ContadorSolicitudesEnMemoria(reloj)).incrementar("login|1.1.1.1", MINUTO);

        assertThat(n).isEqualTo(7);
        verify(redis).execute(ContadorSolicitudesRedis.INCREMENTAR, List.of("oncologia:rl:login|1.1.1.1"), "60000");
    }

    @Test
    void siRedisNoRespondeSigueLimitandoEnMemoria() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.execute(eq(ContadorSolicitudesRedis.INCREMENTAR), anyList(), any(Object[].class)))
                .thenThrow(new RedisConnectionFailureException("caido"));
        var contador = new ContadorSolicitudesRedis(redis, new ContadorSolicitudesEnMemoria(reloj));

        contador.incrementar("login|1.1.1.1", MINUTO);
        assertThat(contador.incrementar("login|1.1.1.1", MINUTO)).isEqualTo(2);
    }

    @Test
    void elFiltroResponde429ConRetryAfterAlSuperarElLimiteDelLogin() throws Exception {
        var filtro = new RateLimitFilter(new ContadorSolicitudesEnMemoria(reloj));
        MockHttpServletResponse ultima = null;
        for (int i = 0; i < 11; i++) {
            var request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
            request.setRemoteAddr("9.9.9.9");
            ultima = new MockHttpServletResponse();
            filtro.doFilter(request, ultima, new MockFilterChain());
        }

        assertThat(ultima.getStatus()).isEqualTo(429);
        assertThat(ultima.getHeader("Retry-After")).isEqualTo("60");
        assertThat(ultima.getContentAsString()).contains("DEMASIADAS_SOLICITUDES");
    }
}

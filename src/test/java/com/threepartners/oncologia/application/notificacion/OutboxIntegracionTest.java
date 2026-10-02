package com.threepartners.oncologia.application.notificacion;

import com.threepartners.oncologia.domain.notificacion.EntregaExternaPort;
import com.threepartners.oncologia.domain.notificacion.EstadoEventoSaliente;
import com.threepartners.oncologia.domain.notificacion.EventoSaliente;
import com.threepartners.oncologia.domain.notificacion.EventoSalienteRepositoryPort;
import com.threepartners.oncologia.domain.notificacion.NotificadorExternoPort;
import com.threepartners.oncologia.domain.shared.ZonaHoraria;
import com.threepartners.oncologia.soporte.PostgresIntegracionTest;
import com.threepartners.oncologia.soporte.RelojDePrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Outbox hacia n8n contra PostgreSQL: el aviso existe solo si la transaccion
 * que lo origino se confirmo, se entrega con reintentos y nunca dos veces a
 * la vez, y no guarda el payload (correos, enlaces con token) mas de lo
 * necesario.
 */
class OutboxIntegracionTest extends PostgresIntegracionTest {

    private static final Instant INICIO = Instant.parse("2026-10-05T15:00:00Z");

    @Autowired private JdbcTemplate jdbc;
    @Autowired private TransactionTemplate tx;
    @Autowired private NotificadorExternoPort notificador;
    @Autowired private EventoSalienteRepositoryPort outbox;

    private final RelojDePrueba reloj = new RelojDePrueba(INICIO, ZonaHoraria.LIMA);
    private final List<String> entregados = new ArrayList<>();
    private boolean n8nCaido;
    private EntregarEventosSalientesUseCase relevo;

    @BeforeEach
    void preparar() {
        jdbc.execute("TRUNCATE evento_saliente RESTART IDENTITY");
        EntregaExternaPort n8n = (destino, payload) -> {
            if (n8nCaido) {
                throw new IllegalStateException("Connection refused");
            }
            entregados.add(destino + " " + payload);
        };
        relevo = new EntregarEventosSalientesUseCase(outbox, n8n, reloj);
    }

    @Test
    void siLaTransaccionQueOriginaElAvisoSeRevierteElAvisoNoExiste() {
        assertThatThrownBy(() -> tx.executeWithoutResult(estado -> {
            notificador.dispararWorkflow("/webhook/consultas/escalada", Map.of("consultaId", 7));
            throw new IllegalStateException("fallo despues de escalar");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(contar()).isZero();
    }

    @Test
    void unAvisoConfirmadoSeEntregaUnaVezYSeVaciaSuPayload() {
        tx.executeWithoutResult(estado ->
                notificador.dispararWorkflow("/webhook/consultas/escalada", Map.of("consultaId", 7, "canal", "TELEGRAM")));

        var resultado = relevo.ejecutar();

        assertThat(resultado.enviados()).isEqualTo(1);
        assertThat(entregados).singleElement().asString().contains("/webhook/consultas/escalada").contains("consultaId=7");
        assertThat(jdbc.queryForObject("SELECT estado || ' ' || payload::text FROM evento_saliente", String.class))
                .isEqualTo("ENVIADO {}");
        assertThat(relevo.ejecutar().enviados()).isZero();
    }

    @Test
    void conN8nCaidoReintentaConEsperaCrecienteYTerminaEnFallido() {
        tx.executeWithoutResult(estado -> notificador.dispararWorkflow("/webhook/notificaciones/recuperar-password",
                Map.of("email", "ana@correo.pe", "urlRestablecer", "https://x/?token=secreto")));
        n8nCaido = true;

        assertThat(relevo.ejecutar().reintentos()).isEqualTo(1);
        assertThat(relevo.ejecutar().reintentos()).as("antes de la espera no se reintenta").isZero();

        List<Duration> esperas = new ArrayList<>();
        for (int intento = 2; intento <= EventoSaliente.MAXIMO_INTENTOS; intento++) {
            Instant proximo = jdbc.queryForObject("SELECT proximo_intento_en FROM evento_saliente", OffsetDateTime.class).toInstant();
            esperas.add(Duration.between(reloj.instant(), proximo));
            reloj.fijar(proximo);
            relevo.ejecutar();
        }

        assertThat(esperas).containsExactly(Duration.ofMinutes(1), Duration.ofMinutes(2), Duration.ofMinutes(4),
                Duration.ofMinutes(8), Duration.ofMinutes(16));
        Map<String, Object> fila = jdbc.queryForMap("SELECT estado, intentos, payload::text AS payload, ultimo_error FROM evento_saliente");
        assertThat(fila.get("estado")).isEqualTo("FALLIDO");
        assertThat(((Number) fila.get("intentos")).intValue()).isEqualTo(EventoSaliente.MAXIMO_INTENTOS);
        assertThat(fila.get("payload")).as("ni el correo ni el token quedan guardados").isEqualTo("{}");
        assertThat((String) fila.get("ultimo_error")).contains("Connection refused");
    }

    @Test
    void unReclamoNoDevuelveLoQueOtraInstanciaYaTomo() {
        tx.executeWithoutResult(estado -> {
            notificador.dispararWorkflow("/webhook/a", Map.of("n", 1));
            notificador.dispararWorkflow("/webhook/b", Map.of("n", 2));
        });

        List<EventoSaliente> primera = outbox.reclamarPendientes(10, INICIO);
        List<EventoSaliente> segunda = outbox.reclamarPendientes(10, INICIO);

        assertThat(primera).extracting(EventoSaliente::destino).containsExactly("/webhook/a", "/webhook/b");
        assertThat(segunda).isEmpty();
    }

    @Test
    void loQueQuedoEnEnvioPorUnaInstanciaCaidaVuelveALaColaYLoEnviadoViejoSePurga() {
        tx.executeWithoutResult(estado -> notificador.dispararWorkflow("/webhook/a", Map.of("n", 1)));
        outbox.reclamarPendientes(10, INICIO);   // la instancia "se cae" sin registrar el resultado

        reloj.avanzar(Duration.ofMinutes(6));
        var resultado = relevo.ejecutar();

        assertThat(resultado.enviados()).isEqualTo(1);
        reloj.avanzar(Duration.ofDays(31));
        assertThat(relevo.ejecutar().purgados()).isEqualTo(1);
        assertThat(outbox.contarPorEstado()).containsEntry(EstadoEventoSaliente.ENVIADO, 0L);
    }

    private int contar() {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM evento_saliente", Integer.class);
        return n != null ? n : 0;
    }
}

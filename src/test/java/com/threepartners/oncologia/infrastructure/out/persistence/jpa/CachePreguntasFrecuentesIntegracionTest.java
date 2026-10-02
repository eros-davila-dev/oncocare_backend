package com.threepartners.oncologia.infrastructure.out.persistence.jpa;

import com.threepartners.oncologia.domain.chatbot.PreguntaFrecuente;
import com.threepartners.oncologia.domain.chatbot.PreguntaFrecuenteRepositoryPort;
import com.threepartners.oncologia.soporte.PostgresIntegracionTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * La base de conocimiento cacheada nunca queda desactualizada tras guardar
 * desde la intranet, y una lectura dentro de la transaccion que guarda no
 * deja cacheado un dato que aun no se confirmo.
 */
class CachePreguntasFrecuentesIntegracionTest extends PostgresIntegracionTest {

    @Autowired private PreguntaFrecuenteRepositoryPort repositorio;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private TransactionTemplate tx;
    @Autowired private CacheManager cacheManager;

    @BeforeEach
    void limpiar() {
        jdbc.execute("TRUNCATE pregunta_frecuente RESTART IDENTITY CASCADE");
        cacheManager.getCacheNames().forEach(nombre -> cacheManager.getCache(nombre).clear());
    }

    @Test
    void laSegundaLecturaSaleDeLaCacheYGuardarLaInvalida() {
        repositorio.guardar(pregunta("¿Horario?", "Lunes a viernes de 8 a 18"));
        assertThat(repositorio.listarActivas()).hasSize(1);

        // Un cambio por fuera del adaptador no se ve: la lectura sale de la cache.
        jdbc.update("UPDATE pregunta_frecuente SET respuesta = 'cambiada a mano'");
        assertThat(repositorio.listarActivas().getFirst().getRespuesta()).isEqualTo("Lunes a viernes de 8 a 18");

        repositorio.guardar(pregunta("¿Estacionamiento?", "Si, gratuito"));
        assertThat(repositorio.listarActivas()).hasSize(2);
    }

    @Test
    void siLaTransaccionQueGuardaSeRevierteLaCacheNoSeContamina() {
        repositorio.guardar(pregunta("¿Horario?", "Lunes a viernes"));
        assertThat(repositorio.listarActivas()).hasSize(1);

        tx.executeWithoutResult(estado -> {
            repositorio.guardar(pregunta("¿Temporal?", "No deberia verse"));
            estado.setRollbackOnly();
        });

        assertThat(repositorio.listarActivas()).extracting(PreguntaFrecuente::getPregunta).containsExactly("¿Horario?");
    }

    private static PreguntaFrecuente pregunta(String pregunta, String respuesta) {
        return PreguntaFrecuente.builder().pregunta(pregunta).respuesta(respuesta).categoria("GENERAL").orden(1).activa(true).build();
    }
}

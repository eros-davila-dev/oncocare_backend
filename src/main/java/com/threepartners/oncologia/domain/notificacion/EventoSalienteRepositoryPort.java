package com.threepartners.oncologia.domain.notificacion;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Persistencia del outbox. {@link #encolar} se ejecuta dentro de la
 * transaccion del caso de uso que origina el aviso: si esa transaccion se
 * revierte, el aviso tampoco existe.
 */
public interface EventoSalienteRepositoryPort {

    void encolar(String destino, Map<String, Object> payload, Instant ahora);

    /**
     * Marca como EN_ENVIO y devuelve los pendientes vencidos, de forma atomica
     * y sin bloquear a otras instancias (FOR UPDATE SKIP LOCKED): dos backends
     * nunca reclaman el mismo aviso.
     */
    List<EventoSaliente> reclamarPendientes(int limite, Instant ahora);

    void marcarEnviado(long id, Instant ahora);

    void reprogramar(long id, int intentos, Instant proximoIntento, String error);

    void marcarFallido(long id, int intentos, String error);

    /** Devuelve a PENDIENTE lo que quedo EN_ENVIO por una instancia que se cayo. */
    int liberarColgados(Instant tomadosAntesDe);

    int purgarEnviados(Instant enviadosAntesDe);

    Map<EstadoEventoSaliente, Long> contarPorEstado();
}

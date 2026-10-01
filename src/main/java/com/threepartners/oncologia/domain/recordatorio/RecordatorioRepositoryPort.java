package com.threepartners.oncologia.domain.recordatorio;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface RecordatorioRepositoryPort {

    Recordatorio guardar(Recordatorio recordatorio);

    Optional<Recordatorio> buscarPorId(Long id);

    boolean existe(Long citaId, TipoRecordatorio tipo, CanalRecordatorio canal);

    /**
     * Pendientes de un canal cuyo momento ya llego, bloqueados para esta
     * transaccion (FOR UPDATE SKIP LOCKED): dos ejecuciones simultaneas de n8n
     * nunca toman el mismo aviso.
     */
    List<Recordatorio> bloquearPendientesVencidos(CanalRecordatorio canal, Instant ahora, int limite);

    /** Pendientes cuyo momento ya llego, sin bloquear (lista de llamadas de recepcion). */
    List<Recordatorio> listarPendientesVencidos(CanalRecordatorio canal, Instant ahora);

    /** EN_PROCESO tomados antes del limite (n8n no informo el resultado). */
    List<Recordatorio> enProcesoTomadosAntesDe(Instant limite);

    List<Recordatorio> listarPorCita(Long citaId);
}

package com.threepartners.oncologia.infrastructure.out.persistence.jpa;

import com.threepartners.oncologia.domain.recordatorio.CanalRecordatorio;
import com.threepartners.oncologia.domain.recordatorio.EstadoRecordatorio;
import com.threepartners.oncologia.domain.recordatorio.TipoRecordatorio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface RecordatorioJpaRepository extends JpaRepository<RecordatorioJpaEntity, Long> {

    boolean existsByCitaIdAndTipoAndCanal(Long citaId, TipoRecordatorio tipo, CanalRecordatorio canal);

    /**
     * SKIP LOCKED: si dos ejecuciones de n8n piden pendientes a la vez, cada
     * una recibe avisos distintos y ninguno se envia dos veces.
     */
    @Query(value = """
            SELECT * FROM recordatorio
            WHERE estado = 'PENDIENTE' AND canal = :canal AND programado_para <= :ahora
            ORDER BY programado_para
            LIMIT :limite
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<RecordatorioJpaEntity> bloquearPendientesVencidos(@Param("canal") String canal, @Param("ahora") Instant ahora,
                                                           @Param("limite") int limite);

    List<RecordatorioJpaEntity> findByEstadoAndCanalAndProgramadoParaLessThanEqualOrderByProgramadoPara(
            EstadoRecordatorio estado, CanalRecordatorio canal, Instant ahora);

    List<RecordatorioJpaEntity> findByEstadoAndTomadoEnBefore(EstadoRecordatorio estado, Instant limite);

    List<RecordatorioJpaEntity> findByCitaIdOrderByProgramadoPara(Long citaId);
}

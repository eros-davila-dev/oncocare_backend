package com.threepartners.oncologia.infrastructure.out.persistence.jpa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface AuditoriaJpaRepository extends JpaRepository<AuditoriaJpaEntity, Long> {

    /**
     * Consulta nativa con cast explicito: PostgreSQL no puede inferir el tipo
     * de un parametro NULL comparado contra una columna timestamp via JPQL
     * (error 42P18), a diferencia de LocalDate (cita/ciclo_tratamiento), que
     * si lo resuelve. El cast a ::timestamp elimina la ambiguedad.
     */
    @Query(
            value = """
                    SELECT * FROM auditoria_accion a
                    WHERE (CAST(:usuarioId AS bigint) IS NULL OR a.usuario_id = CAST(:usuarioId AS bigint))
                      AND (CAST(:entidadAfectada AS varchar) IS NULL OR a.entidad_afectada = CAST(:entidadAfectada AS varchar))
                      AND (CAST(:desde AS timestamp) IS NULL OR a.fecha >= CAST(:desde AS timestamp))
                      AND (CAST(:hasta AS timestamp) IS NULL OR a.fecha <= CAST(:hasta AS timestamp))
                    """,
            countQuery = """
                    SELECT COUNT(*) FROM auditoria_accion a
                    WHERE (CAST(:usuarioId AS bigint) IS NULL OR a.usuario_id = CAST(:usuarioId AS bigint))
                      AND (CAST(:entidadAfectada AS varchar) IS NULL OR a.entidad_afectada = CAST(:entidadAfectada AS varchar))
                      AND (CAST(:desde AS timestamp) IS NULL OR a.fecha >= CAST(:desde AS timestamp))
                      AND (CAST(:hasta AS timestamp) IS NULL OR a.fecha <= CAST(:hasta AS timestamp))
                    """,
            nativeQuery = true)
    Page<AuditoriaJpaEntity> buscar(@Param("usuarioId") Long usuarioId, @Param("entidadAfectada") String entidadAfectada,
                                     @Param("desde") Instant desde, @Param("hasta") Instant hasta, Pageable pageable);
}

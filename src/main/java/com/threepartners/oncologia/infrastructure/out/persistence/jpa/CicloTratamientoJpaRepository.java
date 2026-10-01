package com.threepartners.oncologia.infrastructure.out.persistence.jpa;

import com.threepartners.oncologia.domain.tratamiento.EstadoCicloTratamiento;
import com.threepartners.oncologia.domain.tratamiento.TipoTratamiento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface CicloTratamientoJpaRepository extends JpaRepository<CicloTratamientoJpaEntity, Long> {

    List<CicloTratamientoJpaEntity> findByPacienteIdOrderByFechaSesionAsc(Long pacienteId);

    long countByEstadoAndFechaSesionBetween(EstadoCicloTratamiento estado, LocalDate desde, LocalDate hasta);

    @Query("""
            SELECT c FROM CicloTratamientoJpaEntity c
            WHERE (:pacienteId IS NULL OR c.pacienteId = :pacienteId)
              AND (:tipo IS NULL OR c.tipoTratamiento = :tipo)
            """)
    Page<CicloTratamientoJpaEntity> filtrar(@Param("pacienteId") Long pacienteId, @Param("tipo") TipoTratamiento tipo, Pageable pageable);

    @Query("SELECT AVG(CAST(c.numeroSesion AS double) * 100.0 / c.totalSesionesEsquema) FROM CicloTratamientoJpaEntity c WHERE c.totalSesionesEsquema > 0")
    Double promedioCumplimiento();
}

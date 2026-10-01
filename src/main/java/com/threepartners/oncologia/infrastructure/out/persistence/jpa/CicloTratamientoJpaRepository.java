package com.threepartners.oncologia.infrastructure.out.persistence.jpa;

import com.threepartners.oncologia.domain.tratamiento.EstadoCicloTratamiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface CicloTratamientoJpaRepository extends JpaRepository<CicloTratamientoJpaEntity, Long>,
        JpaSpecificationExecutor<CicloTratamientoJpaEntity> {

    List<CicloTratamientoJpaEntity> findByPacienteIdOrderByFechaSesionAsc(Long pacienteId);

    long countByEstadoAndFechaSesionBetween(EstadoCicloTratamiento estado, LocalDate desde, LocalDate hasta);


    @Query("SELECT AVG(CAST(c.numeroSesion AS double) * 100.0 / c.totalSesionesEsquema) FROM CicloTratamientoJpaEntity c WHERE c.totalSesionesEsquema > 0")
    Double promedioCumplimiento();
}

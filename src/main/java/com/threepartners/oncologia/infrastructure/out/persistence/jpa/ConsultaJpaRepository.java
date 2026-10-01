package com.threepartners.oncologia.infrastructure.out.persistence.jpa;

import com.threepartners.oncologia.domain.estudio.ResultadoConsulta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ConsultaJpaRepository extends JpaRepository<ConsultaJpaEntity, Long>,
        JpaSpecificationExecutor<ConsultaJpaEntity> {

    Optional<ConsultaJpaEntity> findFirstBySesionIdOrderByAbiertaEnDescIdDesc(String sesionId);

    Page<ConsultaJpaEntity> findByResultado(ResultadoConsulta resultado, Pageable pageable);

    List<ConsultaJpaEntity> findByResultadoIsNullAndUltimaActividadEnBefore(Instant limite);

    List<ConsultaJpaEntity> findByResultadoAndUltimaActividadEnBefore(ResultadoConsulta resultado, Instant limite);
}

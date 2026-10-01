package com.threepartners.oncologia.infrastructure.out.persistence.jpa;

import com.threepartners.oncologia.domain.estudio.CanalConsulta;
import com.threepartners.oncologia.domain.estudio.ResultadoConsulta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface ConsultaJpaRepository extends JpaRepository<ConsultaJpaEntity, Long> {

    @Query("""
            SELECT c FROM ConsultaJpaEntity c
            WHERE (:desde IS NULL OR c.abiertaEn >= :desde)
              AND (:hasta IS NULL OR c.abiertaEn < :hasta)
              AND (:canal IS NULL OR c.canal = :canal)
              AND (:resultado IS NULL OR c.resultado = :resultado)
            """)
    Page<ConsultaJpaEntity> filtrar(@Param("desde") Instant desde, @Param("hasta") Instant hasta,
                                    @Param("canal") CanalConsulta canal,
                                    @Param("resultado") ResultadoConsulta resultado, Pageable pageable);
}

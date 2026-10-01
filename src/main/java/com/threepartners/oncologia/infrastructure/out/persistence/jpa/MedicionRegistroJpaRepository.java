package com.threepartners.oncologia.infrastructure.out.persistence.jpa;

import com.threepartners.oncologia.domain.estudio.CanalMedicion;
import com.threepartners.oncologia.domain.estudio.EstadoMedicion;
import com.threepartners.oncologia.domain.estudio.TipoMedicion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface MedicionRegistroJpaRepository extends JpaRepository<MedicionRegistroJpaEntity, Long> {

    @Modifying
    @Query("""
            UPDATE MedicionRegistroJpaEntity m
            SET m.estado = com.threepartners.oncologia.domain.estudio.EstadoMedicion.ABANDONADA
            WHERE m.estado = com.threepartners.oncologia.domain.estudio.EstadoMedicion.EN_CURSO
              AND m.inicio < :limite
            """)
    int marcarAbandonadasIniciadasAntesDe(@Param("limite") Instant limite);

    boolean existsByPacienteIdAndTipoAndInicioAndCanal(Long pacienteId, TipoMedicion tipo, Instant inicio,
                                                       CanalMedicion canal);

    @Query("""
            SELECT m FROM MedicionRegistroJpaEntity m
            WHERE (:desde IS NULL OR m.inicio >= :desde)
              AND (:hasta IS NULL OR m.inicio < :hasta)
              AND (:tipo IS NULL OR m.tipo = :tipo)
              AND (:canal IS NULL OR m.canal = :canal)
              AND (:estado IS NULL OR m.estado = :estado)
            """)
    Page<MedicionRegistroJpaEntity> filtrar(@Param("desde") Instant desde, @Param("hasta") Instant hasta,
                                            @Param("tipo") TipoMedicion tipo, @Param("canal") CanalMedicion canal,
                                            @Param("estado") EstadoMedicion estado, Pageable pageable);
}

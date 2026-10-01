package com.threepartners.oncologia.infrastructure.out.persistence.jpa;

import com.threepartners.oncologia.domain.estudio.CanalMedicion;
import com.threepartners.oncologia.domain.estudio.TipoMedicion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface MedicionRegistroJpaRepository extends JpaRepository<MedicionRegistroJpaEntity, Long>,
        JpaSpecificationExecutor<MedicionRegistroJpaEntity> {

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

}

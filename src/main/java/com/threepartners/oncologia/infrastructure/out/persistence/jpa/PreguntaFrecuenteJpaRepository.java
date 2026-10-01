package com.threepartners.oncologia.infrastructure.out.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PreguntaFrecuenteJpaRepository extends JpaRepository<PreguntaFrecuenteJpaEntity, Long> {

    List<PreguntaFrecuenteJpaEntity> findByActivaTrueOrderByOrdenAscIdAsc();

    List<PreguntaFrecuenteJpaEntity> findAllByOrderByOrdenAscIdAsc();
}

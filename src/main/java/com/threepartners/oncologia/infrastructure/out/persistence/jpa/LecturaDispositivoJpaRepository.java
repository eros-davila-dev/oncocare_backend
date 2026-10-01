package com.threepartners.oncologia.infrastructure.out.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LecturaDispositivoJpaRepository extends JpaRepository<LecturaDispositivoJpaEntity, Long> {

    List<LecturaDispositivoJpaEntity> findByPacienteIdOrderByFechaLecturaDesc(Long pacienteId);

    List<LecturaDispositivoJpaEntity> findByCicloTratamientoIdOrderByFechaLecturaDesc(Long cicloTratamientoId);
}

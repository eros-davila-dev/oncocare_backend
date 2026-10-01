package com.threepartners.oncologia.infrastructure.out.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentoPacienteJpaRepository extends JpaRepository<DocumentoPacienteJpaEntity, Long> {

    List<DocumentoPacienteJpaEntity> findByPacienteIdOrderByFechaCargaDesc(Long pacienteId);
}

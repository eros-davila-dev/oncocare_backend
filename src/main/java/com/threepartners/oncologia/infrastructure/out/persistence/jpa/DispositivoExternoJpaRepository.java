package com.threepartners.oncologia.infrastructure.out.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DispositivoExternoJpaRepository extends JpaRepository<DispositivoExternoJpaEntity, Long> {

    Optional<DispositivoExternoJpaEntity> findByCredencialHash(String credencialHash);
}

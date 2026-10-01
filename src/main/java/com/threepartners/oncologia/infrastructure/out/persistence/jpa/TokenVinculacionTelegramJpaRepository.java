package com.threepartners.oncologia.infrastructure.out.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TokenVinculacionTelegramJpaRepository extends JpaRepository<TokenVinculacionTelegramJpaEntity, Long> {

    Optional<TokenVinculacionTelegramJpaEntity> findByTokenHash(String tokenHash);
}

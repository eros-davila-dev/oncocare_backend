package com.threepartners.oncologia.infrastructure.out.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface SesionRefreshTokenJpaRepository extends JpaRepository<SesionRefreshTokenJpaEntity, Long> {

    Optional<SesionRefreshTokenJpaEntity> findByTokenHash(String tokenHash);

    @Modifying
    @Query("UPDATE SesionRefreshTokenJpaEntity s SET s.revocadoEn = :ahora WHERE s.id = :id")
    void revocarPorId(@Param("id") Long id, @Param("ahora") Instant ahora);

    @Modifying
    @Query("UPDATE SesionRefreshTokenJpaEntity s SET s.revocadoEn = :ahora WHERE s.usuarioId = :usuarioId AND s.revocadoEn IS NULL")
    void revocarTodasDeUsuario(@Param("usuarioId") Long usuarioId, @Param("ahora") Instant ahora);
}

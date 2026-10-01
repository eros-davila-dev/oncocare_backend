package com.threepartners.oncologia.infrastructure.out.persistence.jpa;

import com.threepartners.oncologia.domain.usuario.TipoTokenCuenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TokenAccionCuentaJpaRepository extends JpaRepository<TokenAccionCuentaJpaEntity, Long> {

    Optional<TokenAccionCuentaJpaEntity> findByTokenHash(String tokenHash);

    List<TokenAccionCuentaJpaEntity> findByUsuarioIdAndTipoAndUsadoEnIsNull(Long usuarioId, TipoTokenCuenta tipo);

    @Modifying
    @Query("UPDATE TokenAccionCuentaJpaEntity t SET t.usadoEn = :ahora WHERE t.id IN :ids")
    void marcarUsados(@Param("ids") List<Long> ids, @Param("ahora") Instant ahora);
}

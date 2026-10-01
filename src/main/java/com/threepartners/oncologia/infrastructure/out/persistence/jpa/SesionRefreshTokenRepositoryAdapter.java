package com.threepartners.oncologia.infrastructure.out.persistence.jpa;

import com.threepartners.oncologia.domain.usuario.SesionRefreshToken;
import com.threepartners.oncologia.domain.usuario.SesionRefreshTokenRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SesionRefreshTokenRepositoryAdapter implements SesionRefreshTokenRepositoryPort {

    private final SesionRefreshTokenJpaRepository jpaRepository;

    @Override
    public SesionRefreshToken guardar(SesionRefreshToken sesion) {
        return aDominio(jpaRepository.save(aEntidad(sesion)));
    }

    @Override
    public Optional<SesionRefreshToken> buscarPorHash(String tokenHash) {
        return jpaRepository.findByTokenHash(tokenHash).map(SesionRefreshTokenRepositoryAdapter::aDominio);
    }

    @Override
    public void revocar(Long id) {
        jpaRepository.revocarPorId(id, Instant.now());
    }

    @Override
    public void revocarTodasDeUsuario(Long usuarioId) {
        jpaRepository.revocarTodasDeUsuario(usuarioId, Instant.now());
    }

    private static SesionRefreshTokenJpaEntity aEntidad(SesionRefreshToken sesion) {
        return SesionRefreshTokenJpaEntity.builder()
                .id(sesion.getId())
                .usuarioId(sesion.getUsuarioId())
                .tokenHash(sesion.getTokenHash())
                .expiraEn(sesion.getExpiraEn())
                .revocadoEn(sesion.getRevocadoEn())
                .ipOrigen(sesion.getIpOrigen())
                .creadoEn(sesion.getCreadoEn())
                .build();
    }

    private static SesionRefreshToken aDominio(SesionRefreshTokenJpaEntity entidad) {
        return SesionRefreshToken.builder()
                .id(entidad.getId())
                .usuarioId(entidad.getUsuarioId())
                .tokenHash(entidad.getTokenHash())
                .expiraEn(entidad.getExpiraEn())
                .revocadoEn(entidad.getRevocadoEn())
                .ipOrigen(entidad.getIpOrigen())
                .creadoEn(entidad.getCreadoEn())
                .build();
    }
}

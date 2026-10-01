package com.threepartners.oncologia.infrastructure.out.persistence.jpa;

import com.threepartners.oncologia.domain.usuario.TipoTokenCuenta;
import com.threepartners.oncologia.domain.usuario.TokenAccionCuenta;
import com.threepartners.oncologia.domain.usuario.TokenAccionCuentaRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TokenAccionCuentaRepositoryAdapter implements TokenAccionCuentaRepositoryPort {

    private final TokenAccionCuentaJpaRepository jpaRepository;

    @Override
    public TokenAccionCuenta guardar(TokenAccionCuenta token) {
        return aDominio(jpaRepository.save(aEntidad(token)));
    }

    @Override
    public Optional<TokenAccionCuenta> buscarPorHash(String tokenHash) {
        return jpaRepository.findByTokenHash(tokenHash).map(TokenAccionCuentaRepositoryAdapter::aDominio);
    }

    @Override
    public void marcarUsado(Long id) {
        jpaRepository.marcarUsados(List.of(id), Instant.now());
    }

    @Override
    public void invalidarPendientes(Long usuarioId, TipoTokenCuenta tipo) {
        List<Long> ids = jpaRepository.findByUsuarioIdAndTipoAndUsadoEnIsNull(usuarioId, tipo).stream()
                .map(TokenAccionCuentaJpaEntity::getId)
                .toList();
        if (!ids.isEmpty()) {
            jpaRepository.marcarUsados(ids, Instant.now());
        }
    }

    private static TokenAccionCuentaJpaEntity aEntidad(TokenAccionCuenta token) {
        return TokenAccionCuentaJpaEntity.builder()
                .id(token.getId())
                .usuarioId(token.getUsuarioId())
                .tipo(token.getTipo())
                .tokenHash(token.getTokenHash())
                .expiraEn(token.getExpiraEn())
                .usadoEn(token.getUsadoEn())
                .creadoEn(token.getCreadoEn())
                .build();
    }

    private static TokenAccionCuenta aDominio(TokenAccionCuentaJpaEntity entidad) {
        return TokenAccionCuenta.builder()
                .id(entidad.getId())
                .usuarioId(entidad.getUsuarioId())
                .tipo(entidad.getTipo())
                .tokenHash(entidad.getTokenHash())
                .expiraEn(entidad.getExpiraEn())
                .usadoEn(entidad.getUsadoEn())
                .creadoEn(entidad.getCreadoEn())
                .build();
    }
}

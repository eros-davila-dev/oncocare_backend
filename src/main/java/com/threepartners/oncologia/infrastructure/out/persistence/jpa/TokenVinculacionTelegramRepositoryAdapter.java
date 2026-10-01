package com.threepartners.oncologia.infrastructure.out.persistence.jpa;

import com.threepartners.oncologia.domain.paciente.TokenVinculacionTelegram;
import com.threepartners.oncologia.domain.paciente.TokenVinculacionTelegramRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TokenVinculacionTelegramRepositoryAdapter implements TokenVinculacionTelegramRepositoryPort {

    private final TokenVinculacionTelegramJpaRepository jpaRepository;

    @Override
    public TokenVinculacionTelegram guardar(TokenVinculacionTelegram t) {
        var existente = t.getId() != null ? jpaRepository.findById(t.getId()).orElse(null) : null;
        return aDominio(jpaRepository.save(TokenVinculacionTelegramJpaEntity.builder()
                .id(t.getId())
                .pacienteId(t.getPacienteId())
                .tokenHash(t.getTokenHash())
                .expiraEn(t.getExpiraEn())
                .usadoEn(t.getUsadoEn())
                .creadoEn(existente != null ? existente.getCreadoEn() : Instant.now())
                .build()));
    }

    @Override
    public Optional<TokenVinculacionTelegram> buscarPorHash(String tokenHash) {
        return jpaRepository.findByTokenHash(tokenHash).map(TokenVinculacionTelegramRepositoryAdapter::aDominio);
    }

    private static TokenVinculacionTelegram aDominio(TokenVinculacionTelegramJpaEntity e) {
        return TokenVinculacionTelegram.builder()
                .id(e.getId())
                .pacienteId(e.getPacienteId())
                .tokenHash(e.getTokenHash())
                .expiraEn(e.getExpiraEn())
                .usadoEn(e.getUsadoEn())
                .build();
    }
}

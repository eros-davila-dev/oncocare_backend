package com.threepartners.oncologia.domain.paciente;

import java.util.Optional;

public interface TokenVinculacionTelegramRepositoryPort {

    TokenVinculacionTelegram guardar(TokenVinculacionTelegram token);

    Optional<TokenVinculacionTelegram> buscarPorHash(String tokenHash);
}

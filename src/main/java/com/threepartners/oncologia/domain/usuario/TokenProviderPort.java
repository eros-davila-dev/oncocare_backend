package com.threepartners.oncologia.domain.usuario;

import java.util.Optional;

public interface TokenProviderPort {

    String generarAccessToken(Usuario usuario);

    TokenGenerado generarRefreshToken(Usuario usuario);

    Optional<TokenClaims> validar(String token);
}

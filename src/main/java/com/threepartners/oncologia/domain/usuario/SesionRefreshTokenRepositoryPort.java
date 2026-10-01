package com.threepartners.oncologia.domain.usuario;

import java.util.Optional;

public interface SesionRefreshTokenRepositoryPort {

    SesionRefreshToken guardar(SesionRefreshToken sesion);

    Optional<SesionRefreshToken> buscarPorHash(String tokenHash);

    void revocar(Long id);

    void revocarTodasDeUsuario(Long usuarioId);
}

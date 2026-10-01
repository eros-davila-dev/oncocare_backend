package com.threepartners.oncologia.domain.usuario;

import java.util.Optional;

public interface TokenAccionCuentaRepositoryPort {

    TokenAccionCuenta guardar(TokenAccionCuenta token);

    Optional<TokenAccionCuenta> buscarPorHash(String tokenHash);

    void marcarUsado(Long id);

    /**
     * Invalida (marca como usados) los tokens pendientes de un tipo para un
     * usuario, para que solo el ultimo token emitido sea valido (ej. al pedir
     * un nuevo enlace de recuperacion de contrasena).
     */
    void invalidarPendientes(Long usuarioId, TipoTokenCuenta tipo);
}

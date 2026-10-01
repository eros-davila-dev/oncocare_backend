package com.threepartners.oncologia.domain.shared.exception;

public class TokenInvalidoException extends DomainException {

    public TokenInvalidoException() {
        super("El enlace no es valido o ha expirado");
    }
}

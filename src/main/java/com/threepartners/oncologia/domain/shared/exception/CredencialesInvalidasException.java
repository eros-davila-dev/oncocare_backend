package com.threepartners.oncologia.domain.shared.exception;

public class CredencialesInvalidasException extends DomainException {

    public CredencialesInvalidasException() {
        super("Credenciales invalidas");
    }
}

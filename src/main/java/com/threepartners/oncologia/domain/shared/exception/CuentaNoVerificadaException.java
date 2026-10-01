package com.threepartners.oncologia.domain.shared.exception;

public class CuentaNoVerificadaException extends DomainException {

    public CuentaNoVerificadaException() {
        super("Debes verificar tu correo electronico antes de iniciar sesion");
    }
}

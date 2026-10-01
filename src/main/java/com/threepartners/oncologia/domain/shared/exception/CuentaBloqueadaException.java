package com.threepartners.oncologia.domain.shared.exception;

public class CuentaBloqueadaException extends DomainException {

    public CuentaBloqueadaException() {
        super("La cuenta esta temporalmente bloqueada por multiples intentos fallidos. Intenta nuevamente mas tarde");
    }
}

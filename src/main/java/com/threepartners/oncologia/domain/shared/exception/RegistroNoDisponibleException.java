package com.threepartners.oncologia.domain.shared.exception;

public class RegistroNoDisponibleException extends DomainException {

    public RegistroNoDisponibleException() {
        super("El registro de nuevas cuentas de paciente estara disponible proximamente. Intenta mas tarde.");
    }
}

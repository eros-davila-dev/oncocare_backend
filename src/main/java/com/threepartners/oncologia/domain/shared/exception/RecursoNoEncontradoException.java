package com.threepartners.oncologia.domain.shared.exception;

public class RecursoNoEncontradoException extends DomainException {

    public RecursoNoEncontradoException(String recurso, Object identificador) {
        super("%s no encontrado con identificador: %s".formatted(recurso, identificador));
    }
}

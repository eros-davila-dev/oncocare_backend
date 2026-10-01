package com.threepartners.oncologia.domain.auditoria.event;

import com.threepartners.oncologia.domain.auditoria.ResultadoAuditoria;

public record PasswordRestablecidoEvent(
        Long usuarioId,
        String entidadId,
        String ipOrigen,
        ResultadoAuditoria resultado
) implements AuditoriaEvent {

    @Override
    public String accion() {
        return "PASSWORD_RESTABLECIDO";
    }

    @Override
    public String entidadAfectada() {
        return "USUARIO";
    }

    @Override
    public String valoresPrevios() {
        return null;
    }

    @Override
    public String valoresNuevos() {
        return null;
    }
}

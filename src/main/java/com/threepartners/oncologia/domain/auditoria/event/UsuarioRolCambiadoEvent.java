package com.threepartners.oncologia.domain.auditoria.event;

import com.threepartners.oncologia.domain.auditoria.ResultadoAuditoria;

public record UsuarioRolCambiadoEvent(
        Long usuarioId,
        String entidadId,
        String valoresPrevios,
        String valoresNuevos,
        String ipOrigen,
        ResultadoAuditoria resultado
) implements AuditoriaEvent {

    @Override
    public String accion() {
        return "USUARIO_ROL_CAMBIADO";
    }

    @Override
    public String entidadAfectada() {
        return "USUARIO";
    }
}

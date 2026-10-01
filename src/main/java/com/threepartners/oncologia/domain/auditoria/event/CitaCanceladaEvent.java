package com.threepartners.oncologia.domain.auditoria.event;

import com.threepartners.oncologia.domain.auditoria.ResultadoAuditoria;

public record CitaCanceladaEvent(
        Long usuarioId,
        String entidadId,
        String valoresPrevios,
        String ipOrigen,
        ResultadoAuditoria resultado
) implements AuditoriaEvent {

    @Override
    public String accion() {
        return "CITA_CANCELADA";
    }

    @Override
    public String entidadAfectada() {
        return "CITA";
    }

    @Override
    public String valoresNuevos() {
        return null;
    }
}

package com.threepartners.oncologia.domain.auditoria.event;

import com.threepartners.oncologia.domain.auditoria.ResultadoAuditoria;

public record CicloTratamientoProgramadoEvent(
        Long usuarioId,
        String entidadId,
        String valoresNuevos,
        String ipOrigen,
        ResultadoAuditoria resultado
) implements AuditoriaEvent {

    @Override
    public String accion() {
        return "CICLO_TRATAMIENTO_PROGRAMADO";
    }

    @Override
    public String entidadAfectada() {
        return "CICLO_TRATAMIENTO";
    }

    @Override
    public String valoresPrevios() {
        return null;
    }
}

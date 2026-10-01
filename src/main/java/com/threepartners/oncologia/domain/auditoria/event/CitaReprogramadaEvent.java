package com.threepartners.oncologia.domain.auditoria.event;

import com.threepartners.oncologia.domain.auditoria.ResultadoAuditoria;

public record CitaReprogramadaEvent(
        Long usuarioId,
        String entidadId,
        String valoresPrevios,
        String valoresNuevos,
        String ipOrigen,
        ResultadoAuditoria resultado
) implements AuditoriaEvent {

    @Override
    public String accion() {
        return "CITA_REPROGRAMADA";
    }

    @Override
    public String entidadAfectada() {
        return "CITA";
    }
}

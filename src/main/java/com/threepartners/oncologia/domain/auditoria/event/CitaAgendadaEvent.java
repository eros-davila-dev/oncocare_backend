package com.threepartners.oncologia.domain.auditoria.event;

import com.threepartners.oncologia.domain.auditoria.ResultadoAuditoria;

public record CitaAgendadaEvent(
        Long usuarioId,
        String entidadId,
        String valoresNuevos,
        String ipOrigen,
        ResultadoAuditoria resultado
) implements AuditoriaEvent {

    @Override
    public String accion() {
        return "CITA_AGENDADA";
    }

    @Override
    public String entidadAfectada() {
        return "CITA";
    }

    @Override
    public String valoresPrevios() {
        return null;
    }
}

package com.threepartners.oncologia.domain.auditoria.event;

import com.threepartners.oncologia.domain.auditoria.ResultadoAuditoria;

public record PacienteActualizadoEvent(
        Long usuarioId,
        String entidadId,
        String valoresPrevios,
        String valoresNuevos,
        String ipOrigen,
        ResultadoAuditoria resultado
) implements AuditoriaEvent {

    @Override
    public String accion() {
        return "PACIENTE_ACTUALIZADO";
    }

    @Override
    public String entidadAfectada() {
        return "PACIENTE";
    }
}

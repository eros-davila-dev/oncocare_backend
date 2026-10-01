package com.threepartners.oncologia.domain.auditoria.event;

import com.threepartners.oncologia.domain.auditoria.ResultadoAuditoria;

public record PacienteRegistradoEvent(
        Long usuarioId,
        String entidadId,
        String valoresNuevos,
        String ipOrigen,
        ResultadoAuditoria resultado
) implements AuditoriaEvent {

    @Override
    public String accion() {
        return "PACIENTE_REGISTRADO";
    }

    @Override
    public String entidadAfectada() {
        return "PACIENTE";
    }

    @Override
    public String valoresPrevios() {
        return null;
    }
}

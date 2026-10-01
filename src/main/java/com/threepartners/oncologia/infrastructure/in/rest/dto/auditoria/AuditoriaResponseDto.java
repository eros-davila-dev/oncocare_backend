package com.threepartners.oncologia.infrastructure.in.rest.dto.auditoria;

import com.threepartners.oncologia.domain.auditoria.ResultadoAuditoria;

import java.time.Instant;

public record AuditoriaResponseDto(
        Long id,
        Long usuarioId,
        String accion,
        String entidadAfectada,
        String entidadId,
        String valoresPrevios,
        String valoresNuevos,
        String ipOrigen,
        Instant fecha,
        ResultadoAuditoria resultado
) {
}

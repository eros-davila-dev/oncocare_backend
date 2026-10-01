package com.threepartners.oncologia.domain.auditoria.event;

import com.threepartners.oncologia.domain.auditoria.ResultadoAuditoria;

/**
 * Evento auditable generico para las operaciones del modulo de estudio y de
 * la agenda (configurar fases, incluir/excluir participantes, capturas del
 * pretest, importaciones, anulaciones, desenlaces de citas). Evita una clase
 * por accion cuando el evento no tiene mas datos que los del contrato comun.
 */
public record OperacionAuditadaEvent(
        Long usuarioId,
        String accion,
        String entidadAfectada,
        String entidadId,
        String valoresPrevios,
        String valoresNuevos,
        String ipOrigen,
        ResultadoAuditoria resultado
) implements AuditoriaEvent {

    public static OperacionAuditadaEvent exito(Long usuarioId, String accion, String entidad, Object entidadId,
                                               String valoresPrevios, String valoresNuevos, String ipOrigen) {
        return new OperacionAuditadaEvent(usuarioId, accion, entidad,
                entidadId != null ? String.valueOf(entidadId) : null,
                valoresPrevios, valoresNuevos, ipOrigen, ResultadoAuditoria.EXITO);
    }
}

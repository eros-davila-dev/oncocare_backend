package com.threepartners.oncologia.domain.auditoria;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Registro inmutable de auditoria. Solo se crea, nunca se modifica ni se elimina.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditoriaAccion {

    private Long id;
    private Long usuarioId;
    private String accion;
    private String entidadAfectada;
    private String entidadId;
    private String valoresPrevios;
    private String valoresNuevos;
    private String ipOrigen;
    private Instant fecha;
    private ResultadoAuditoria resultado;
}

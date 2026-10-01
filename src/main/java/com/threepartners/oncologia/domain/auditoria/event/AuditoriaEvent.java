package com.threepartners.oncologia.domain.auditoria.event;

import com.threepartners.oncologia.domain.auditoria.ResultadoAuditoria;

/**
 * Contrato comun de todo evento de dominio auditable. AuditoriaEventListener
 * escucha cualquier implementacion de esta interfaz sin conocer los casos de
 * uso que las publican.
 */
public interface AuditoriaEvent {

    Long usuarioId();

    String accion();

    String entidadAfectada();

    String entidadId();

    String valoresPrevios();

    String valoresNuevos();

    String ipOrigen();

    ResultadoAuditoria resultado();
}

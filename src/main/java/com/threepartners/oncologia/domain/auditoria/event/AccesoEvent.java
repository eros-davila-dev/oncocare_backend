package com.threepartners.oncologia.domain.auditoria.event;

import com.threepartners.oncologia.domain.auditoria.ResultadoAuditoria;

/**
 * Cubre tanto inicios de sesion exitosos como intentos fallidos (RNF de seguridad,
 * seccion 12: "intentos de acceso fallidos" tambien se auditan).
 */
public record AccesoEvent(
        Long usuarioId,
        String entidadId,
        String ipOrigen,
        ResultadoAuditoria resultado
) implements AuditoriaEvent {

    @Override
    public String accion() {
        return resultado == ResultadoAuditoria.EXITO ? "LOGIN_EXITOSO" : "LOGIN_FALLIDO";
    }

    @Override
    public String entidadAfectada() {
        return "USUARIO";
    }

    @Override
    public String valoresPrevios() {
        return null;
    }

    @Override
    public String valoresNuevos() {
        return null;
    }
}

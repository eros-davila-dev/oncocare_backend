package com.threepartners.oncologia.domain.usuario;

/**
 * Ciclo de vida de la cuenta de autoservicio (seccion 7). No reemplaza el
 * flag {@code activo} de {@link Usuario} (que sigue siendo el interruptor de
 * habilitado/deshabilitado usado por el staff): este estado solo cubre el
 * paso de verificacion de correo que antecede al primer login.
 */
public enum EstadoCuenta {
    PENDIENTE_VERIFICACION,
    ACTIVA
}

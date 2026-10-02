package com.threepartners.oncologia.domain.notificacion;

/**
 * Correos transaccionales del sistema. Cada uno tiene su plantilla (sin datos
 * clinicos: solo nombre, enlace y vigencia) en la capa de infraestructura.
 */
public enum TipoCorreo {
    /** Al registrarse en el portal: enlace para verificar el correo. */
    VERIFICACION_EMAIL,
    /** "Olvide mi contrasena": enlace para elegir una nueva. */
    RESTABLECER_PASSWORD,
    /** Cuando un administrador crea un usuario: acceso y enlace para elegir su contrasena. */
    BIENVENIDA_USUARIO
}

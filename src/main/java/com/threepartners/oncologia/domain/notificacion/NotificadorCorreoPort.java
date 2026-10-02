package com.threepartners.oncologia.domain.notificacion;

/**
 * Envio de correos transaccionales. Como {@link NotificadorExternoPort}, el
 * correo se guarda en el outbox dentro de la transaccion del llamador y se
 * envia despues con reintentos: si la transaccion se revierte (por ejemplo,
 * la cuenta no se llego a guardar) el correo no sale, y una caida del
 * proveedor no hace fallar el registro ni la recuperacion de contrasena.
 */
public interface NotificadorCorreoPort {

    /**
     * @param nombre nombre a mostrar en el saludo (solo el nombre de pila se usa)
     * @param enlace URL de accion (verificar, restablecer, ingresar)
     */
    void enviar(TipoCorreo tipo, String email, String nombre, String enlace);
}

package com.threepartners.oncologia.infrastructure.out.correo;

/**
 * Un transporte de correo. Si no puede entregar, lanza una excepcion: el
 * ServicioCorreo prueba el siguiente proveedor y, si ninguno puede, el
 * outbox reintenta mas tarde.
 */
public interface ProveedorCorreo {

    String nombre();

    void enviar(MensajeCorreo mensaje, String remitente, String nombreRemitente);
}

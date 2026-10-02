package com.threepartners.oncologia.infrastructure.out.correo;

import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Envia por el primer proveedor que funcione, en orden: API de correo y
 * luego SMTP (cada uno solo si su bandera esta activa). Si ninguno puede, la
 * excepcion llega al outbox, que reintenta mas tarde. Los logs no incluyen el
 * destinatario (dato personal).
 */
@Slf4j
public class ServicioCorreo {

    private final List<ProveedorCorreo> proveedores;
    private final String remitente;
    private final String nombreRemitente;

    public ServicioCorreo(List<ProveedorCorreo> proveedores, String remitente, String nombreRemitente) {
        this.proveedores = List.copyOf(proveedores);
        this.remitente = remitente;
        this.nombreRemitente = nombreRemitente;
    }

    public boolean habilitado() {
        return !proveedores.isEmpty();
    }

    public List<String> proveedoresActivos() {
        return proveedores.stream().map(ProveedorCorreo::nombre).toList();
    }

    public void enviar(MensajeCorreo mensaje) {
        if (proveedores.isEmpty()) {
            throw new IllegalStateException("No hay proveedor de correo activo (MAIL_FLAG_QUIPU / MAIL_FLAG_SMTP)");
        }
        RuntimeException ultimoError = null;
        for (ProveedorCorreo proveedor : proveedores) {
            try {
                proveedor.enviar(mensaje, remitente, nombreRemitente);
                if (ultimoError != null) {
                    log.info("Correo \"{}\" enviado por el proveedor de respaldo {}", mensaje.asunto(), proveedor.nombre());
                }
                return;
            } catch (RuntimeException e) {
                log.warn("El proveedor de correo {} no pudo enviar \"{}\": {}", proveedor.nombre(), mensaje.asunto(),
                        e.getClass().getSimpleName() + ": " + e.getMessage());
                ultimoError = e;
            }
        }
        throw ultimoError;
    }
}

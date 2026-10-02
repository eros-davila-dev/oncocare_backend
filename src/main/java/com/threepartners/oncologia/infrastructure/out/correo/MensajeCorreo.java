package com.threepartners.oncologia.infrastructure.out.correo;

/** Correo listo para enviar (HTML ya renderizado). */
public record MensajeCorreo(String destinatario, String asunto, String html) {
}

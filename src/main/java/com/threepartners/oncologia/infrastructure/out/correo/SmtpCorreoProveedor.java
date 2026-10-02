package com.threepartners.oncologia.infrastructure.out.correo;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

/** SMTP (spring.mail.*): respaldo de la API de correo. */
public class SmtpCorreoProveedor implements ProveedorCorreo {

    private final JavaMailSender mailSender;

    public SmtpCorreoProveedor(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public String nombre() {
        return "smtp";
    }

    @Override
    public void enviar(MensajeCorreo mensaje, String remitente, String nombreRemitente) {
        try {
            MimeMessage mime = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(mime, false, StandardCharsets.UTF_8.name());
            helper.setFrom(remitente, nombreRemitente);
            helper.setTo(mensaje.destinatario());
            helper.setSubject(mensaje.asunto());
            helper.setText(mensaje.html(), true);
            mailSender.send(mime);
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new IllegalStateException("No se pudo armar el correo SMTP: " + e.getMessage(), e);
        }
    }
}

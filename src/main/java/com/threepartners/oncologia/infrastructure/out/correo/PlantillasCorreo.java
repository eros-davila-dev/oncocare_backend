package com.threepartners.oncologia.infrastructure.out.correo;

import com.threepartners.oncologia.domain.notificacion.TipoCorreo;
import org.springframework.web.util.HtmlUtils;

/**
 * Plantillas de los correos transaccionales. Solo llevan el nombre de pila y
 * el enlace (minimizacion, Ley 29733): nunca datos clinicos ni el documento.
 * HTML con estilos en linea (los clientes de correo ignoran las hojas de
 * estilo) y el enlace tambien como texto, por si el boton no se muestra.
 */
public final class PlantillasCorreo {

    private static final String INSTITUCION = "Fundación Oncológica Three Partners";

    private PlantillasCorreo() {
    }

    public static MensajeCorreo renderizar(TipoCorreo tipo, String destinatario, String nombre, String enlace) {
        String saludo = "Hola" + (nombrePila(nombre).isEmpty() ? "" : " " + HtmlUtils.htmlEscape(nombrePila(nombre))) + ",";
        return switch (tipo) {
            case VERIFICACION_EMAIL -> new MensajeCorreo(destinatario, "Confirma tu correo para activar tu cuenta",
                    html(saludo,
                            "Recibimos tu registro en el portal de pacientes. Para activar tu cuenta, confirma que este correo es tuyo.",
                            "Confirmar mi correo", enlace,
                            "El enlace vence en 24 horas. Si no fuiste tú, ignora este mensaje: la cuenta no se activará."));
            case RESTABLECER_PASSWORD -> new MensajeCorreo(destinatario, "Restablece tu contraseña",
                    html(saludo,
                            "Recibimos una solicitud para restablecer la contraseña de tu cuenta.",
                            "Elegir una nueva contraseña", enlace,
                            "El enlace vence en 1 hora y solo puede usarse una vez. Si no lo pediste, ignora este mensaje: "
                                    + "tu contraseña actual sigue funcionando."));
            case BIENVENIDA_USUARIO -> new MensajeCorreo(destinatario, "Tu cuenta en el sistema de la fundación",
                    html(saludo,
                            "Se creó tu cuenta en el sistema de gestión de pacientes. Para empezar, elige tu propia contraseña.",
                            "Elegir mi contraseña", enlace,
                            "El enlace vence en 72 horas y solo puede usarse una vez. Si vence, usa \"¿Olvidaste tu contraseña?\" "
                                    + "en la pantalla de ingreso."));
        };
    }

    static String nombrePila(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return "";
        }
        return nombre.trim().split("\\s+")[0];
    }

    private static String html(String saludo, String cuerpo, String textoBoton, String enlace, String nota) {
        String enlaceSeguro = HtmlUtils.htmlEscape(enlace);
        return """
                <!doctype html>
                <html lang="es">
                <body style="margin:0;padding:0;background:#f4f6fb;font-family:Arial,Helvetica,sans-serif;color:#1f2937;">
                  <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background:#f4f6fb;padding:24px 0;">
                    <tr><td align="center">
                      <table role="presentation" width="560" cellpadding="0" cellspacing="0"
                             style="max-width:560px;width:100%%;background:#ffffff;border-radius:12px;padding:32px;">
                        <tr><td style="font-size:14px;font-weight:bold;color:#2563eb;padding-bottom:16px;">%s</td></tr>
                        <tr><td style="font-size:18px;font-weight:bold;padding-bottom:12px;">%s</td></tr>
                        <tr><td style="font-size:16px;line-height:24px;padding-bottom:24px;">%s</td></tr>
                        <tr><td style="padding-bottom:24px;">
                          <a href="%s" style="display:inline-block;background:#2563eb;color:#ffffff;text-decoration:none;
                             font-size:16px;font-weight:bold;padding:14px 24px;border-radius:8px;">%s</a>
                        </td></tr>
                        <tr><td style="font-size:14px;line-height:20px;color:#4b5563;padding-bottom:16px;">%s</td></tr>
                        <tr><td style="font-size:12px;line-height:18px;color:#6b7280;">
                          Si el botón no funciona, copia este enlace en tu navegador:<br>
                          <a href="%s" style="color:#2563eb;word-break:break-all;">%s</a>
                        </td></tr>
                      </table>
                      <p style="font-size:12px;color:#9ca3af;margin-top:16px;">%s · Este es un mensaje automático, no respondas a este correo.</p>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(INSTITUCION, saludo, cuerpo, enlaceSeguro, textoBoton, nota, enlaceSeguro, enlaceSeguro, INSTITUCION);
    }
}

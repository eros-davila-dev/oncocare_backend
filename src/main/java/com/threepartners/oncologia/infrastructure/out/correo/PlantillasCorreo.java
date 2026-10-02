package com.threepartners.oncologia.infrastructure.out.correo;

import com.threepartners.oncologia.domain.notificacion.TipoCorreo;
import org.springframework.web.util.HtmlUtils;

/**
 * Plantillas de los correos transaccionales con la identidad de OncoCare.
 *
 * Decisiones de diseño, pensadas para clientes de correo y no para navegadores:
 * - Maquetación con tablas y estilos en línea (Gmail y Outlook ignoran hojas
 *   de estilo y flexbox), ancho máximo de 600 px y fluido en el celular.
 * - El logo va por URL pública (los clientes bloquean SVG y base64). Si el
 *   cliente bloquea imágenes, su texto alternativo se ve como la marca.
 * - Solo el nombre de pila y el enlace (minimización, Ley 29733): nunca
 *   datos clínicos ni el documento.
 * - El enlace también se muestra como texto, por si el botón no aparece.
 */
public final class PlantillasCorreo {

    /** Paleta tomada del logo. */
    private static final String AZUL_MARINO = "#14215B";
    private static final String VIOLETA = "#7C5CE8";
    private static final String AZUL = "#3B7BEA";
    private static final String CELESTE = "#4FB3F6";
    private static final String TEXTO = "#334155";
    private static final String TEXTO_SUAVE = "#64748B";
    private static final String FONDO = "#F1F4FA";
    private static final String FUENTE = "'Segoe UI', Roboto, Helvetica, Arial, sans-serif";

    private static final String MARCA = "OncoCare";
    private static final String INSTITUCION = "Fundación Oncológica Three Partners";
    private static final String LEMA = "Cuidamos hoy tu mañana";

    private PlantillasCorreo() {
    }

    public static MensajeCorreo renderizar(TipoCorreo tipo, String destinatario, String nombre, String enlace) {
        return renderizar(tipo, destinatario, nombre, enlace, null);
    }

    public static MensajeCorreo renderizar(TipoCorreo tipo, String destinatario, String nombre, String enlace,
                                           String logoUrl) {
        Contenido c = contenido(tipo);
        String pila = nombrePila(nombre);
        String saludo = pila.isEmpty() ? "Hola:" : "Hola, " + HtmlUtils.htmlEscape(pila) + ":";
        return new MensajeCorreo(destinatario, c.asunto(), html(c, saludo, enlace, logoUrl));
    }

    private static Contenido contenido(TipoCorreo tipo) {
        return switch (tipo) {
            case VERIFICACION_EMAIL -> new Contenido(
                    "Confirma tu correo para activar tu cuenta",
                    "Activa tu cuenta del portal de pacientes con un clic.",
                    "Activación de cuenta",
                    "Confirma tu correo electrónico",
                    "Gracias por registrarte en el portal de pacientes. Para proteger tu información, necesitamos "
                            + "confirmar que este correo es tuyo antes de activar tu cuenta.",
                    "Confirmar mi correo",
                    "Este enlace vence en <strong>24 horas</strong>.",
                    "Recibes este correo porque se creó una cuenta en el portal de pacientes con esta dirección. "
                            + "Si no fuiste tú, ignóralo: la cuenta no se activará.");
            case RESTABLECER_PASSWORD -> new Contenido(
                    "Restablece tu contraseña",
                    "Usa este enlace para crear una nueva contraseña. Vence en 1 hora.",
                    "Seguridad de tu cuenta",
                    "Restablece tu contraseña",
                    "Recibimos una solicitud para restablecer la contraseña de tu cuenta. Haz clic en el botón "
                            + "para crear una nueva.",
                    "Crear nueva contraseña",
                    "Este enlace vence en <strong>1 hora</strong> y solo puede usarse una vez.",
                    "Si no solicitaste este cambio, ignora este mensaje: tu contraseña actual sigue siendo válida "
                            + "y nadie podrá cambiarla sin este enlace.");
            case BIENVENIDA_USUARIO -> new Contenido(
                    "Bienvenido(a) al sistema de la fundación",
                    "Tu cuenta está lista. Elige tu contraseña para empezar.",
                    "Bienvenida",
                    "Tu cuenta está lista",
                    "El equipo de la fundación creó tu acceso al sistema de gestión de pacientes. Para empezar, "
                            + "elige tu contraseña personal.",
                    "Elegir mi contraseña",
                    "Este enlace vence en <strong>72 horas</strong> y solo puede usarse una vez. Si vence, usa "
                            + "<strong>«¿Olvidaste tu contraseña?»</strong> en la pantalla de ingreso.",
                    "Recibes este correo porque un administrador de la fundación registró tu cuenta.");
        };
    }

    static String nombrePila(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return "";
        }
        return nombre.trim().split("\\s+")[0];
    }

    private static String html(Contenido c, String saludo, String enlace, String logoUrl) {
        String enlaceSeguro = HtmlUtils.htmlEscape(enlace);
        return PLANTILLA
                .replace("{{asunto}}", HtmlUtils.htmlEscape(c.asunto()))
                .replace("{{preencabezado}}", HtmlUtils.htmlEscape(c.preencabezado()))
                .replace("{{cabecera}}", cabecera(logoUrl))
                .replace("{{etiqueta}}", c.etiqueta())
                .replace("{{titulo}}", c.titulo())
                .replace("{{saludo}}", saludo)
                .replace("{{cuerpo}}", c.cuerpo())
                .replace("{{boton}}", c.boton())
                .replace("{{vigencia}}", c.vigencia())
                .replace("{{motivo}}", c.motivo())
                .replace("{{enlace}}", enlaceSeguro)
                .replace("{{azulMarino}}", AZUL_MARINO)
                .replace("{{violeta}}", VIOLETA)
                .replace("{{azul}}", AZUL)
                .replace("{{celeste}}", CELESTE)
                .replace("{{texto}}", TEXTO)
                .replace("{{textoSuave}}", TEXTO_SUAVE)
                .replace("{{fondo}}", FONDO)
                .replace("{{fuente}}", FUENTE)
                .replace("{{marca}}", MARCA)
                .replace("{{institucion}}", INSTITUCION)
                .replace("{{lema}}", LEMA);
    }

    /** Logo si hay URL; si no, la marca en texto con los colores del logo. */
    private static String cabecera(String logoUrl) {
        if (logoUrl == null || logoUrl.isBlank()) {
            return "<span style=\"font-family:{{fuente}};font-size:30px;font-weight:800;letter-spacing:-0.5px;\">"
                    + "<span style=\"color:{{azulMarino}};\">Onco</span><span style=\"color:{{azul}};\">Care</span></span>"
                    + "<br><span style=\"font-family:{{fuente}};font-size:11px;letter-spacing:3px;color:{{textoSuave}};\">"
                    + "CUIDAMOS HOY TU MAÑANA</span>";
        }
        return "<img src=\"" + HtmlUtils.htmlEscape(logoUrl) + "\" width=\"200\" alt=\"OncoCare\""
                + " style=\"display:block;width:200px;max-width:70%;height:auto;margin:0 auto;border:0;"
                + "font-family:{{fuente}};font-size:30px;font-weight:800;line-height:40px;text-align:center;color:{{azulMarino}};\">";
    }

    private record Contenido(String asunto, String preencabezado, String etiqueta, String titulo, String cuerpo,
                             String boton, String vigencia, String motivo) {
    }

    private static final String PLANTILLA = """
            <!doctype html>
            <html lang="es" xmlns="http://www.w3.org/1999/xhtml">
            <head>
            <meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <meta name="color-scheme" content="light">
            <meta name="supported-color-schemes" content="light">
            <title>{{asunto}}</title>
            </head>
            <body style="margin:0;padding:0;background-color:{{fondo}};-webkit-text-size-adjust:100%;">
            <div style="display:none;max-height:0;overflow:hidden;opacity:0;color:transparent;">{{preencabezado}}</div>
            <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" bgcolor="{{fondo}}" style="background-color:{{fondo}};">
              <tr>
                <td align="center" style="padding:32px 12px;">
                  <table role="presentation" width="600" cellpadding="0" cellspacing="0" border="0" style="width:100%;max-width:600px;">

                    <!-- Cabecera con el logo -->
                    <tr>
                      <td align="center" bgcolor="#FFFFFF" style="background-color:#FFFFFF;border-radius:16px 16px 0 0;padding:28px 24px 20px;">
                        {{cabecera}}
                      </td>
                    </tr>
                    <!-- Franja con los colores del logo -->
                    <tr>
                      <td style="font-size:0;line-height:0;">
                        <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0">
                          <tr>
                            <td height="5" width="34%" bgcolor="{{violeta}}" style="background-color:{{violeta}};height:5px;font-size:0;line-height:0;">&nbsp;</td>
                            <td height="5" width="33%" bgcolor="{{azul}}" style="background-color:{{azul}};height:5px;font-size:0;line-height:0;">&nbsp;</td>
                            <td height="5" width="33%" bgcolor="{{celeste}}" style="background-color:{{celeste}};height:5px;font-size:0;line-height:0;">&nbsp;</td>
                          </tr>
                        </table>
                      </td>
                    </tr>

                    <!-- Contenido -->
                    <tr>
                      <td bgcolor="#FFFFFF" style="background-color:#FFFFFF;padding:36px 40px 12px;font-family:{{fuente}};">
                        <p style="margin:0 0 8px;font-size:12px;font-weight:700;letter-spacing:1.5px;text-transform:uppercase;color:{{violeta}};">{{etiqueta}}</p>
                        <h1 style="margin:0 0 20px;font-size:24px;line-height:32px;font-weight:700;color:{{azulMarino}};">{{titulo}}</h1>
                        <p style="margin:0 0 12px;font-size:16px;line-height:26px;color:{{texto}};">{{saludo}}</p>
                        <p style="margin:0 0 28px;font-size:16px;line-height:26px;color:{{texto}};">{{cuerpo}}</p>

                        <!-- Boton -->
                        <table role="presentation" cellpadding="0" cellspacing="0" border="0" style="margin:0 0 28px;">
                          <tr>
                            <td align="center" bgcolor="{{azul}}" style="border-radius:10px;background-color:{{azul}};background-image:linear-gradient(90deg,{{violeta}},{{azul}});">
                              <a href="{{enlace}}" target="_blank" style="display:inline-block;padding:15px 32px;font-family:{{fuente}};font-size:16px;font-weight:700;line-height:20px;color:#FFFFFF;text-decoration:none;border-radius:10px;">{{boton}}</a>
                            </td>
                          </tr>
                        </table>

                        <!-- Vigencia -->
                        <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="margin:0 0 24px;">
                          <tr>
                            <td bgcolor="#F4F1FE" style="background-color:#F4F1FE;border-left:4px solid {{violeta}};border-radius:8px;padding:14px 18px;font-family:{{fuente}};font-size:14px;line-height:22px;color:{{texto}};">
                              {{vigencia}}
                            </td>
                          </tr>
                        </table>

                        <p style="margin:0 0 24px;font-size:14px;line-height:22px;color:{{textoSuave}};">{{motivo}}</p>

                        <!-- Enlace en texto -->
                        <p style="margin:0 0 6px;font-size:13px;line-height:20px;color:{{textoSuave}};">¿El botón no funciona? Copia y pega este enlace en tu navegador:</p>
                        <p style="margin:0 0 28px;font-size:13px;line-height:20px;word-break:break-all;"><a href="{{enlace}}" target="_blank" style="color:{{azul}};text-decoration:underline;">{{enlace}}</a></p>
                      </td>
                    </tr>

                    <!-- Aviso de seguridad -->
                    <tr>
                      <td bgcolor="#FFFFFF" style="background-color:#FFFFFF;padding:0 40px 32px;font-family:{{fuente}};">
                        <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0">
                          <tr>
                            <td style="border-top:1px solid #E2E8F0;padding-top:20px;font-size:13px;line-height:20px;color:{{textoSuave}};">
                              <strong style="color:{{azulMarino}};">Cuidamos tu información.</strong>
                              El personal de la fundación nunca te pedirá tu contraseña por correo, teléfono ni Telegram.
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>

                    <!-- Pie -->
                    <tr>
                      <td align="center" bgcolor="{{azulMarino}}" style="background-color:{{azulMarino}};border-radius:0 0 16px 16px;padding:24px 32px;font-family:{{fuente}};">
                        <p style="margin:0 0 4px;font-size:15px;font-weight:700;color:#FFFFFF;">{{marca}} · {{institucion}}</p>
                        <p style="margin:0 0 14px;font-size:12px;letter-spacing:2px;text-transform:uppercase;color:#A5B4FC;">{{lema}}</p>
                        <p style="margin:0;font-size:12px;line-height:18px;color:#CBD5E1;">Lima, Perú · Mensaje automático, por favor no respondas a este correo.<br>Si necesitas ayuda, escríbenos desde el portal o el asistente virtual.</p>
                      </td>
                    </tr>

                  </table>
                </td>
              </tr>
            </table>
            </body>
            </html>
            """;
}

package com.threepartners.oncologia.infrastructure.out.correo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import com.threepartners.oncologia.domain.notificacion.TipoCorreo;
import com.threepartners.oncologia.infrastructure.out.notification.EntregaExternaEnrutador;
import com.threepartners.oncologia.infrastructure.out.notification.N8nNotificationAdapter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class CorreoTest {

    private final ObjectMapper json = new ObjectMapper();
    private HttpServer servidor;

    @AfterEach
    void detener() {
        if (servidor != null) {
            servidor.stop(0);
        }
    }

    @Test
    void laApiRecibeElTokenEnLaCabeceraYElCuerpoQueEsperaElProveedor() throws Exception {
        AtomicReference<String> token = new AtomicReference<>();
        AtomicReference<String> cuerpo = new AtomicReference<>();
        String url = levantar(200, "{\"status\":\"success\"}", token, cuerpo);

        new ApiCorreoProveedor(url, "clave-secreta", json).enviar(
                new MensajeCorreo("ana@correo.pe", "Asunto", "<p>Hola</p>"), "no-responder@fundacion.pe", "Fundacion");

        assertThat(token.get()).isEqualTo("clave-secreta");
        JsonNode enviado = json.readTree(cuerpo.get());
        assertThat(enviado.get("from").asText()).isEqualTo("no-responder@fundacion.pe");
        assertThat(enviado.get("fromName").asText()).isEqualTo("Fundacion");
        assertThat(enviado.get("to").get(0).asText()).isEqualTo("ana@correo.pe");
        assertThat(enviado.get("subject").asText()).isEqualTo("Asunto");
        assertThat(enviado.get("body_html").asText()).isEqualTo("<p>Hola</p>");
    }

    @Test
    void laApiFallaSiRespondeErrorOSiNoConfirmaElEnvio() throws Exception {
        String conError = levantar(500, "{\"message\":\"caido\"}", new AtomicReference<>(), new AtomicReference<>());
        var mensaje = new MensajeCorreo("ana@correo.pe", "Asunto", "<p>x</p>");
        assertThatThrownBy(() -> new ApiCorreoProveedor(conError, "k", json).enviar(mensaje, "a@b.pe", "F"))
                .isInstanceOf(RuntimeException.class);

        servidor.stop(0);
        String sinConfirmar = levantar(200, "{\"status\":\"error\",\"message\":\"cuota agotada\"}",
                new AtomicReference<>(), new AtomicReference<>());
        assertThatThrownBy(() -> new ApiCorreoProveedor(sinConfirmar, "k", json).enviar(mensaje, "a@b.pe", "F"))
                .hasMessageContaining("cuota agotada");
    }

    @Test
    void sinUrlOClaveLaApiNoArranca() {
        assertThatThrownBy(() -> new ApiCorreoProveedor("", "k", json)).hasMessageContaining("MAIL_API_URL");
        assertThatThrownBy(() -> new ApiCorreoProveedor("https://x/v1/send-email", " ", json))
                .hasMessageContaining("MAIL_API_KEY");
    }

    @Test
    void siLaApiFallaUsaSmtpYSiTodosFallanPropagaElError() {
        List<String> usados = new ArrayList<>();
        ProveedorCorreo apiCaida = proveedor("api", usados, true);
        ProveedorCorreo smtp = proveedor("smtp", usados, false);

        new ServicioCorreo(List.of(apiCaida, smtp), "a@b.pe", "F").enviar(new MensajeCorreo("x@y.pe", "S", "h"));
        assertThat(usados).containsExactly("api", "smtp");

        var soloCaida = new ServicioCorreo(List.of(apiCaida), "a@b.pe", "F");
        assertThatThrownBy(() -> soloCaida.enviar(new MensajeCorreo("x@y.pe", "S", "h"))).hasMessageContaining("api caido");
        assertThatThrownBy(() -> new ServicioCorreo(List.of(), "a@b.pe", "F").enviar(new MensajeCorreo("x@y.pe", "S", "h")))
                .hasMessageContaining("MAIL_FLAG_QUIPU");
    }

    @Test
    void lasPlantillasSaludanConElNombreDePilaEscapadoYLlevanElEnlace() {
        MensajeCorreo m = PlantillasCorreo.renderizar(TipoCorreo.RESTABLECER_PASSWORD, "ana@correo.pe",
                "Ana <b>Maria</b> Perez", "https://portal.pe/auth/restablecer-password?token=abc&x=1");

        assertThat(m.asunto()).isEqualTo("Restablece tu contraseña");
        assertThat(m.html()).contains("Hola, Ana:").doesNotContain("Perez").doesNotContain("<b>Maria");
        assertThat(m.html()).contains("href=\"https://portal.pe/auth/restablecer-password?token=abc&amp;x=1\"");
        assertThat(PlantillasCorreo.renderizar(TipoCorreo.VERIFICACION_EMAIL, "a@b.pe", "<script>", "https://x").html())
                .doesNotContain("<script>");
        for (TipoCorreo tipo : TipoCorreo.values()) {
            String html = PlantillasCorreo.renderizar(tipo, "a@b.pe", null, "https://x").html();
            assertThat(html).contains("Hola:").doesNotContain("{{").contains("OncoCare");
        }
    }

    @Test
    void elRecordatorioLlevaFechaYHoraYAlReferidoNoLeMuestraBoton() {
        var datos = java.util.Map.of("fecha", "viernes 9 de octubre", "hora", "09:00", "paciente", "<b>Ana</b> Maria");

        MensajeCorreo alPaciente = PlantillasCorreo.renderizar(TipoCorreo.RECORDATORIO_CITA, "ana@correo.pe", "Ana Maria",
                "https://portal/mis-citas", null, datos);
        assertThat(alPaciente.asunto()).contains("viernes 9 de octubre").contains("09:00");
        assertThat(alPaciente.html()).contains("Hola, Ana:").contains("https://portal/mis-citas").contains("Mis citas");

        MensajeCorreo alReferido = PlantillasCorreo.renderizar(TipoCorreo.RECORDATORIO_CITA_REFERIDO, "hija@correo.pe",
                "Lucia Perez", "", null, datos);
        assertThat(alReferido.html())
                .contains("Hola, Lucia:")
                .contains("&lt;b&gt;Ana&lt;/b&gt;") // solo el nombre de pila, escapado: nunca HTML del usuario
                .doesNotContain("<b>Ana</b>")
                .doesNotContain("Maria")
                .doesNotContain("href=\"\"")
                .doesNotContain("El botón no funciona");
    }

    @Test
    void sinEnlaceElRecordatorioAlPacienteIndicaComunicarseConRecepcion() {
        MensajeCorreo m = PlantillasCorreo.renderizar(TipoCorreo.RECORDATORIO_CITA, "ana@correo.pe", "Ana", "", null,
                java.util.Map.of("fecha", "viernes 9 de octubre", "hora", "09:00"));

        assertThat(m.html()).contains("recepción").doesNotContain("Ver mis citas</a>");
    }

    @Test
    void elOutboxEnviaLosCorreosAlServicioDeCorreoYLoDemasAN8n() {
        N8nNotificationAdapter n8n = mock(N8nNotificationAdapter.class);
        List<MensajeCorreo> enviados = new ArrayList<>();
        ProveedorCorreo capturador = new ProveedorCorreo() {
            @Override
            public String nombre() {
                return "prueba";
            }

            @Override
            public void enviar(MensajeCorreo mensaje, String remitente, String nombreRemitente) {
                enviados.add(mensaje);
            }
        };
        var propiedades = new CorreoProperties("a@b.pe", "F", "https://portal.pe/logo-correo.png", null, null);
        var enrutador = new EntregaExternaEnrutador(n8n, new ServicioCorreo(List.of(capturador), "a@b.pe", "F"), propiedades,
                com.threepartners.oncologia.config.InstitucionProperties.porDefecto());

        enrutador.entregar("correo:BIENVENIDA_USUARIO",
                Map.of("email", "medico@fundacion.pe", "nombre", "Luis", "enlace", "https://intranet/x"));
        assertThat(enviados).singleElement().satisfies(m -> {
            assertThat(m.destinatario()).isEqualTo("medico@fundacion.pe");
            assertThat(m.html()).contains("https://intranet/x").contains("src=\"https://portal.pe/logo-correo.png\"");
        });
        verifyNoInteractions(n8n);

        enrutador.entregar("/webhook/consultas/escalada", Map.of("consultaId", 3));
        verify(n8n).entregar("/webhook/consultas/escalada", Map.of("consultaId", 3));
    }

    private static ProveedorCorreo proveedor(String nombre, List<String> usados, boolean falla) {
        return new ProveedorCorreo() {
            @Override
            public String nombre() {
                return nombre;
            }

            @Override
            public void enviar(MensajeCorreo mensaje, String remitente, String nombreRemitente) {
                usados.add(nombre);
                if (falla) {
                    throw new IllegalStateException(nombre + " caido");
                }
            }
        };
    }

    private String levantar(int estado, String respuesta, AtomicReference<String> token,
                            AtomicReference<String> cuerpo) throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/v1/send-email", intercambio -> {
            token.set(intercambio.getRequestHeaders().getFirst("X-API-Token"));
            cuerpo.set(new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] bytes = respuesta.getBytes(StandardCharsets.UTF_8);
            intercambio.getResponseHeaders().add("Content-Type", "application/json");
            intercambio.sendResponseHeaders(estado, bytes.length);
            intercambio.getResponseBody().write(bytes);
            intercambio.close();
        });
        servidor.start();
        return "http://127.0.0.1:" + servidor.getAddress().getPort() + "/v1/send-email";
    }
}

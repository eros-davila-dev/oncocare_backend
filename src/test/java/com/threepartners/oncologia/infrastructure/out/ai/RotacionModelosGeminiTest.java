package com.threepartners.oncologia.infrastructure.out.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import com.threepartners.oncologia.domain.chatbot.ContextoUsuarioChatbot;
import com.threepartners.oncologia.domain.chatbot.Intencion;
import com.threepartners.oncologia.domain.chatbot.InterpretacionChatbot;
import com.threepartners.oncologia.soporte.RelojDePrueba;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Simula la API de Gemini con un servidor local: cada modelo responde lo que
 * el test le indique (cuota diaria agotada, limite por minuto, inexistente u
 * OK) y se verifica que el chatbot siga respondiendo con el siguiente modelo.
 */
class RotacionModelosGeminiTest {

    /** 2/10/2026 10:00 en Lima = 08:00 en Los Angeles. */
    private static final Instant INICIO = Instant.parse("2026-10-02T15:00:00Z");

    private static final String CUOTA_DIARIA = """
            {"error":{"code":429,"status":"RESOURCE_EXHAUSTED","details":[
              {"@type":"type.googleapis.com/google.rpc.QuotaFailure","violations":[
                {"quotaId":"GenerateRequestsPerDayPerProjectPerModel-FreeTier"}]},
              {"@type":"type.googleapis.com/google.rpc.RetryInfo","retryDelay":"37s"}]}}""";
    private static final String CUOTA_POR_MINUTO = """
            {"error":{"code":429,"status":"RESOURCE_EXHAUSTED","details":[
              {"@type":"type.googleapis.com/google.rpc.QuotaFailure","violations":[
                {"quotaId":"GenerateRequestsPerMinutePerProjectPerModel-FreeTier"}]},
              {"@type":"type.googleapis.com/google.rpc.RetryInfo","retryDelay":"20s"}]}}""";
    private static final String RESPUESTA_OK = """
            {"candidates":[{"content":{"parts":[{"text":"{\\"intencion\\":\\"HELP\\",\\"listoParaEjecutar\\":false,\\"respuesta\\":\\"Hola\\"}"}]}}]}""";

    private final RelojDePrueba reloj = new RelojDePrueba(INICIO, ZoneId.of("America/Lima"));
    private final ObjectMapper json = new ObjectMapper();
    private final Map<String, Respuesta> respuestas = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> llamadas = new ConcurrentHashMap<>();
    private HttpServer servidor;
    private GeminiRestClientAdapter adapter;
    private RotacionModelosGemini rotacion;

    @BeforeEach
    void levantarGeminiFalso() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/v1beta/models/", intercambio -> {
            String modelo = intercambio.getRequestURI().getPath().replaceAll(".*/models/(.*):generateContent", "$1");
            llamadas.computeIfAbsent(modelo, m -> new AtomicInteger()).incrementAndGet();
            intercambio.getRequestBody().readAllBytes();
            Respuesta r = respuestas.getOrDefault(modelo, new Respuesta(200, RESPUESTA_OK));
            if (r.demoraMilis() > 0) {
                try {
                    Thread.sleep(r.demoraMilis());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            byte[] bytes = r.cuerpo().getBytes(StandardCharsets.UTF_8);
            intercambio.getResponseHeaders().add("Content-Type", "application/json");
            intercambio.sendResponseHeaders(r.estado(), bytes.length);
            intercambio.getResponseBody().write(bytes);
            intercambio.close();
        });
        servidor.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
        servidor.start();
        var propiedades = new GeminiProperties("clave", "modelo-a", List.of("modelo-b", "modelo-c"),
                "http://127.0.0.1:" + servidor.getAddress().getPort(), Duration.ofMillis(300), Duration.ofSeconds(5));
        rotacion = new RotacionModelosGemini(reloj, json);
        adapter = new GeminiRestClientAdapter(propiedades, RestClient.builder(), json, rotacion);
    }

    @AfterEach
    void detener() {
        servidor.stop(0);
    }

    @Test
    void siUnModeloAgotaSuCuotaElMismoMensajeLoRespondeElSiguiente() {
        respuestas.put("modelo-a", new Respuesta(429, CUOTA_DIARIA));
        respuestas.put("modelo-b", new Respuesta(429, CUOTA_POR_MINUTO));

        InterpretacionChatbot r = preguntar();

        assertThat(r.intencion()).isEqualTo(Intencion.HELP);
        assertThat(r.respuestaSugerida()).isEqualTo("Hola");
        assertThat(llamadas.get("modelo-c").get()).isEqualTo(1);
    }

    @Test
    void losModelosAgotadosNoSeVuelvenAIntentarHastaQueSuCuotaSeReinicia() {
        respuestas.put("modelo-a", new Respuesta(429, CUOTA_DIARIA));
        respuestas.put("modelo-b", new Respuesta(429, CUOTA_POR_MINUTO));
        preguntar();

        preguntar();
        assertThat(llamadas.get("modelo-a").get()).as("agotado por el dia: no se reintenta").isEqualTo(1);
        assertThat(llamadas.get("modelo-b").get()).as("limite por minuto: espera 20 s").isEqualTo(1);

        respuestas.remove("modelo-b");
        reloj.avanzar(Duration.ofSeconds(25));
        preguntar();
        assertThat(llamadas.get("modelo-b").get()).as("pasaron los 20 s indicados por Google").isEqualTo(2);
        assertThat(llamadas.get("modelo-a").get()).isEqualTo(1);

        // La cuota diaria se reinicia a la medianoche del Pacifico (16 h despues de las 08:00 en Los Angeles)
        respuestas.remove("modelo-a");
        reloj.avanzar(Duration.ofHours(16));
        preguntar();
        assertThat(llamadas.get("modelo-a").get()).isEqualTo(2);
    }

    @Test
    void unModeloInexistenteSeApartaYSiTodosEstanAgotadosRespondeEnModoDegradado() {
        respuestas.put("modelo-a", new Respuesta(404, "{\"error\":{\"code\":404,\"status\":\"NOT_FOUND\"}}"));
        respuestas.put("modelo-b", new Respuesta(429, CUOTA_DIARIA));
        respuestas.put("modelo-c", new Respuesta(429, CUOTA_DIARIA));

        InterpretacionChatbot r = preguntar();
        assertThat(r.intencion()).isEqualTo(Intencion.ESCALATE_TO_STAFF);

        preguntar();
        assertThat(llamadas.values().stream().mapToInt(AtomicInteger::get).sum())
                .as("con todo agotado ya no se llama a Google").isEqualTo(3);
        assertThat(rotacion.disponibles(List.of("modelo-a", "modelo-b", "modelo-c"))).isEmpty();
    }

    @Test
    void unModeloQueSeQuedaColgadoSeSaltaAlVencerSuTiempo() {
        respuestas.put("modelo-a", new Respuesta(200, RESPUESTA_OK, 1_500));

        long inicio = System.nanoTime();
        InterpretacionChatbot r = preguntar();

        assertThat(r.intencion()).isEqualTo(Intencion.HELP);
        assertThat(llamadas.get("modelo-b").get()).isEqualTo(1);
        assertThat(Duration.ofNanos(System.nanoTime() - inicio)).isLessThan(Duration.ofMillis(1_400));
    }

    @Test
    void unModeloSaturadoSeApartaCadaVezMasTiempoHastaQueVuelveAResponder() {
        respuestas.put("modelo-a", new Respuesta(503, "{\"error\":{\"code\":503,\"status\":\"UNAVAILABLE\"}}"));
        List<Duration> esperas = new java.util.ArrayList<>();
        for (int i = 0; i < 3; i++) {
            preguntar();
            Instant desde = reloj.instant();
            while (rotacion.disponibles(List.of("modelo-a")).isEmpty()) {
                reloj.avanzar(Duration.ofSeconds(1));
            }
            esperas.add(Duration.between(desde, reloj.instant()));
        }
        assertThat(esperas).containsExactly(Duration.ofSeconds(30), Duration.ofSeconds(60), Duration.ofSeconds(120));

        respuestas.remove("modelo-a");
        preguntar();
        respuestas.put("modelo-a", new Respuesta(503, "{}"));
        preguntar();
        Instant desde = reloj.instant();
        while (rotacion.disponibles(List.of("modelo-a")).isEmpty()) {
            reloj.avanzar(Duration.ofSeconds(1));
        }
        assertThat(Duration.between(desde, reloj.instant())).as("tras responder bien vuelve a 30 s")
                .isEqualTo(Duration.ofSeconds(30));
    }

    @Test
    void elModeloPreferidoVaPrimeroYNoSeRepiten() {
        var p = new GeminiProperties("k", "gemini-3.6-flash", List.of("gemini-3.8-flash", "gemini-3.6-flash", " ", "gemini-2.5-flash-lite"), "u");
        assertThat(p.modelosEnOrden()).containsExactly("gemini-3.6-flash", "gemini-3.8-flash", "gemini-2.5-flash-lite");

        var sinLista = new GeminiProperties("k", "gemini-3.6-flash", List.of(""), "u");
        assertThat(sinLista.modelosEnOrden()).as("GEMINI_MODELS vacia usa la lista por defecto")
                .hasSize(GeminiProperties.MODELOS_POR_DEFECTO.size()).startsWith("gemini-3.6-flash");
    }

    private InterpretacionChatbot preguntar() {
        return adapter.interpretar("hola", List.of(), new ContextoUsuarioChatbot(false, false, null));
    }

    private record Respuesta(int estado, String cuerpo, long demoraMilis) {
        Respuesta(int estado, String cuerpo) {
            this(estado, cuerpo, 0);
        }
    }
}

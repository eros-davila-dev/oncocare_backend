package com.threepartners.oncologia.infrastructure.out.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.threepartners.oncologia.domain.chatbot.ConfiguracionGemini;
import com.threepartners.oncologia.domain.chatbot.ConfiguracionGeminiRepositoryPort;
import com.threepartners.oncologia.domain.chatbot.ContextoUsuarioChatbot;
import com.threepartners.oncologia.domain.chatbot.Intencion;
import com.threepartners.oncologia.domain.chatbot.InterpretacionChatbot;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class GeminiRestClientAdapterTest {

    @Test
    void respondeEnModoDegradadoSiNoHayApiKeyConfigurada() {
        GeminiProperties properties = new GeminiProperties("", "gemini-2.0-flash", "https://generativelanguage.googleapis.com");
        GeminiRestClientAdapter adapter = adaptador(properties, Optional.empty());

        InterpretacionChatbot resultado = adapter.interpretar("hola", List.of(), new ContextoUsuarioChatbot(false, false, null));

        assertThat(resultado.intencion()).isEqualTo(Intencion.ESCALATE_TO_STAFF);
        assertThat(resultado.listoParaEjecutar()).isFalse();
        assertThat(resultado.respuestaSugerida()).isNotBlank();
    }

    @Test
    void respondeEnModoDegradadoSiLaLlamadaHttpFalla() {
        // Sin servidor real detras, la llamada a un host invalido debe fallar
        // y caer al modo degradado en vez de propagar la excepcion.
        GeminiProperties properties = new GeminiProperties("clave-de-prueba", "gemini-2.0-flash", "http://host-invalido.invalido");
        GeminiRestClientAdapter adapter = adaptador(properties, Optional.empty());

        InterpretacionChatbot resultado = adapter.interpretar("hola", List.of(), new ContextoUsuarioChatbot(false, false, null));

        assertThat(resultado.intencion()).isEqualTo(Intencion.ESCALATE_TO_STAFF);
    }

    @Test
    void usaLaClaveGuardadaEnLaIntranetAunqueNoHayaVariableDeEntorno() {
        // Con clave en la intranet ya no hay modo degradado por falta de clave: se intenta
        // llamar a Google (host invalido) y se degrada recien por la falla de red.
        GeminiProperties properties = new GeminiProperties("", "gemini-2.0-flash", "http://host-invalido.invalido");
        var guardada = new ConfiguracionGemini("clave-intranet", false, List.of("gemini-2.5-flash"), 1L, null);
        RotacionModelosGemini rotacion = new RotacionModelosGemini(java.time.Clock.systemUTC(), new ObjectMapper());
        GeminiRestClientAdapter adapter = new GeminiRestClientAdapter(properties, RestClient.builder(), new ObjectMapper(),
                rotacion, new ResolutorConfiguracionGemini(properties, repositorio(Optional.of(guardada))));

        adapter.interpretar("hola", List.of(), new ContextoUsuarioChatbot(false, false, null));

        // El modelo de la intranet (no el del entorno) quedo apartado por no responder.
        assertThat(rotacion.disponibles(List.of("gemini-2.5-flash"))).isEmpty();
        assertThat(rotacion.disponibles(List.of("gemini-2.0-flash"))).containsExactly("gemini-2.0-flash");
    }

    private static GeminiRestClientAdapter adaptador(GeminiProperties properties, Optional<ConfiguracionGemini> guardada) {
        return new GeminiRestClientAdapter(properties, RestClient.builder(), new ObjectMapper(),
                new RotacionModelosGemini(java.time.Clock.systemUTC(), new ObjectMapper()),
                new ResolutorConfiguracionGemini(properties, repositorio(guardada)));
    }

    static ConfiguracionGeminiRepositoryPort repositorio(Optional<ConfiguracionGemini> guardada) {
        return new ConfiguracionGeminiRepositoryPort() {
            @Override
            public Optional<ConfiguracionGemini> obtener() {
                return guardada;
            }

            @Override
            public void guardar(ConfiguracionGemini configuracion) {
                throw new UnsupportedOperationException();
            }
        };
    }
}

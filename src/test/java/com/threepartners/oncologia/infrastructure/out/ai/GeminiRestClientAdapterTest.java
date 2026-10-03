package com.threepartners.oncologia.infrastructure.out.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.threepartners.oncologia.domain.chatbot.ContextoUsuarioChatbot;
import com.threepartners.oncologia.domain.chatbot.Intencion;
import com.threepartners.oncologia.domain.chatbot.InterpretacionChatbot;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GeminiRestClientAdapterTest {

    @Test
    void respondeEnModoDegradadoSiNoHayApiKeyConfigurada() {
        GeminiProperties properties = new GeminiProperties("", "gemini-2.0-flash", "https://generativelanguage.googleapis.com");
        GeminiRestClientAdapter adapter = new GeminiRestClientAdapter(properties, RestClient.builder(), new ObjectMapper(),
                new RotacionModelosGemini(java.time.Clock.systemUTC(), new ObjectMapper()));

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
        GeminiRestClientAdapter adapter = new GeminiRestClientAdapter(properties, RestClient.builder(), new ObjectMapper(),
                new RotacionModelosGemini(java.time.Clock.systemUTC(), new ObjectMapper()));

        InterpretacionChatbot resultado = adapter.interpretar("hola", List.of(), new ContextoUsuarioChatbot(false, false, null));

        assertThat(resultado.intencion()).isEqualTo(Intencion.ESCALATE_TO_STAFF);
    }
}

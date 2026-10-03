package com.threepartners.oncologia.application.chatbot;

import com.threepartners.oncologia.domain.chatbot.ContextoUsuarioChatbot;
import com.threepartners.oncologia.domain.chatbot.PreguntaFrecuente;
import com.threepartners.oncologia.domain.chatbot.PreguntaFrecuenteRepositoryPort;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class SugerenciasChatbotTest {

    private static SugerenciasChatbot con(String... preguntas) {
        List<PreguntaFrecuente> lista = java.util.Arrays.stream(preguntas)
                .map(p -> PreguntaFrecuente.builder().pregunta(p).activa(true).build()).toList();
        return new SugerenciasChatbot(new PreguntaFrecuenteRepositoryPort() {
            public List<PreguntaFrecuente> listarActivas() { return lista; }
            public List<PreguntaFrecuente> listarTodas() { return lista; }
            public Optional<PreguntaFrecuente> buscarPorId(Long id) { return Optional.empty(); }
            public PreguntaFrecuente guardar(PreguntaFrecuente p) { return p; }
        });
    }

    @Test
    void noRepiteUnaPreguntaFrecuenteQueYaEsUnaSugerencia() {
        var sugerencias = con("¿Cómo creo mi cuenta de paciente?", "¿Cuál es el horario?")
                .iniciales(new ContextoUsuarioChatbot(false, false, null));

        assertThat(sugerencias).containsExactly("Quiero agendar una cita", "¿Cómo creo mi cuenta?", "¿Cuál es el horario?");
    }

    @Test
    void ofreceComoMaximoSeisYTresPreguntasFrecuentes() {
        var sugerencias = con("P1 uno", "P2 dos", "P3 tres", "P4 cuatro", "P5 cinco")
                .iniciales(new ContextoUsuarioChatbot(true, true, "Ana"));

        assertThat(sugerencias).hasSize(6).contains("P1 uno", "P2 dos", "P3 tres").doesNotContain("P4 cuatro");
    }

    @Test
    void comparaSinTildesNiSignos() {
        assertThat(SugerenciasChatbot.parecidas("¿Cómo creo mi cuenta?", "como creo mi CUENTA de paciente")).isTrue();
        assertThat(SugerenciasChatbot.parecidas("Ver mis citas", "¿Cuál es el horario?")).isFalse();
    }
}

package com.threepartners.oncologia.infrastructure.out.ai;

import com.threepartners.oncologia.domain.chatbot.ConfiguracionGemini;
import com.threepartners.oncologia.domain.chatbot.ConfiguracionGeminiRepositoryPort;
import com.threepartners.oncologia.domain.chatbot.ConfiguracionGeminiVigente;
import com.threepartners.oncologia.domain.chatbot.OrigenConfiguracionGemini;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ResolutorConfiguracionGeminiTest {

    private static final GeminiProperties ENTORNO = new GeminiProperties(
            "clave-entorno-1234", "gemini-2.5-flash", List.of("gemini-2.5-flash-lite"), "https://x");

    @Test
    void sinNadaGuardadoUsaLasVariablesDeEntorno() {
        ConfiguracionGeminiVigente v = new ResolutorConfiguracionGemini(ENTORNO, repositorio(Optional.empty())).vigente();

        assertThat(v.apiKey()).isEqualTo("clave-entorno-1234");
        assertThat(v.origenClave()).isEqualTo(OrigenConfiguracionGemini.ENTORNO);
        assertThat(v.modelos()).containsExactly("gemini-2.5-flash", "gemini-2.5-flash-lite");
        assertThat(v.origenModelos()).isEqualTo(OrigenConfiguracionGemini.ENTORNO);
        assertThat(v.claveEnmascarada()).isEqualTo("****1234");
    }

    @Test
    void loGuardadoEnLaIntranetTienePrioridad() {
        var guardada = new ConfiguracionGemini("clave-intranet-abcd", false, List.of("gemini-3-flash", "gemini-2.0-flash"), 1L, null);

        ConfiguracionGeminiVigente v = new ResolutorConfiguracionGemini(ENTORNO, repositorio(Optional.of(guardada))).vigente();

        assertThat(v.apiKey()).isEqualTo("clave-intranet-abcd");
        assertThat(v.origenClave()).isEqualTo(OrigenConfiguracionGemini.INTRANET);
        assertThat(v.modelos()).containsExactly("gemini-3-flash", "gemini-2.0-flash");
        assertThat(v.origenModelos()).isEqualTo(OrigenConfiguracionGemini.INTRANET);
    }

    @Test
    void soloModelosGuardadosMantieneLaClaveDelEntorno() {
        var guardada = new ConfiguracionGemini(null, false, List.of("gemini-3-flash"), 1L, null);

        ConfiguracionGeminiVigente v = new ResolutorConfiguracionGemini(ENTORNO, repositorio(Optional.of(guardada))).vigente();

        assertThat(v.origenClave()).isEqualTo(OrigenConfiguracionGemini.ENTORNO);
        assertThat(v.modelos()).containsExactly("gemini-3-flash");
    }

    @Test
    void claveGuardadaIlegibleCaeAlEntornoYLoAvisa() {
        var guardada = new ConfiguracionGemini(null, true, List.of("gemini-3-flash"), 1L, null);

        ConfiguracionGeminiVigente v = new ResolutorConfiguracionGemini(ENTORNO, repositorio(Optional.of(guardada))).vigente();

        assertThat(v.origenClave()).isEqualTo(OrigenConfiguracionGemini.ENTORNO);
        assertThat(v.claveGuardadaIlegible()).isTrue();
    }

    @Test
    void sinClaveEnNingunLadoQuedaSinConfigurar() {
        var sinClave = new GeminiProperties("", "gemini-2.5-flash", "https://x");

        ConfiguracionGeminiVigente v = new ResolutorConfiguracionGemini(sinClave, repositorio(Optional.empty())).vigente();

        assertThat(v.tieneClave()).isFalse();
        assertThat(v.origenClave()).isEqualTo(OrigenConfiguracionGemini.SIN_CONFIGURAR);
        assertThat(v.claveEnmascarada()).isNull();
    }

    @Test
    void siLaBaseFallaSigueConElEntorno() {
        ConfigurationGeminiRepositorioRoto roto = new ConfigurationGeminiRepositorioRoto();

        ConfiguracionGeminiVigente v = new ResolutorConfiguracionGemini(ENTORNO, roto).vigente();

        assertThat(v.origenClave()).isEqualTo(OrigenConfiguracionGemini.ENTORNO);
    }

    private static ConfiguracionGeminiRepositoryPort repositorio(Optional<ConfiguracionGemini> guardada) {
        return GeminiRestClientAdapterTest.repositorio(guardada);
    }

    private static final class ConfigurationGeminiRepositorioRoto implements ConfiguracionGeminiRepositoryPort {
        @Override
        public Optional<ConfiguracionGemini> obtener() {
            throw new IllegalStateException("base caida");
        }

        @Override
        public void guardar(ConfiguracionGemini configuracion) {
            throw new UnsupportedOperationException();
        }
    }
}

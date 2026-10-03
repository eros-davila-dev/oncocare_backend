package com.threepartners.oncologia.application.chatbot;

import com.threepartners.oncologia.domain.auditoria.event.OperacionAuditadaEvent;
import com.threepartners.oncologia.domain.chatbot.AdministracionGeminiPort;
import com.threepartners.oncologia.domain.chatbot.ConfiguracionGemini;
import com.threepartners.oncologia.domain.chatbot.ConfiguracionGeminiRepositoryPort;
import com.threepartners.oncologia.domain.chatbot.ConfiguracionGeminiVigente;
import com.threepartners.oncologia.domain.chatbot.OrigenConfiguracionGemini;
import com.threepartners.oncologia.domain.shared.exception.ValidacionDeNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfigurarGeminiUseCaseTest {

    @Mock
    private ConfiguracionGeminiRepositoryPort repositorio;
    @Mock
    private AdministracionGeminiPort gemini;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private ConfigurarGeminiUseCase useCase;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-03T15:00:00Z"), ZoneOffset.UTC);
        useCase = new ConfigurarGeminiUseCase(repositorio, gemini, eventPublisher, clock);
        lenient().when(gemini.vigente()).thenReturn(new ConfiguracionGeminiVigente("clave-entorno-9999",
                OrigenConfiguracionGemini.ENTORNO, false, List.of("gemini-2.5-flash"), OrigenConfiguracionGemini.ENTORNO));
    }

    @Test
    void guardaLaClaveNuevaYLosModelosEnOrdenSinRepetidos() {
        when(repositorio.obtener()).thenReturn(Optional.empty());

        useCase.actualizar(new ConfigurarGeminiUseCase.Cambios(" clave-nueva-abcd ", false,
                List.of("gemini-3-flash", " gemini-2.5-flash ", "gemini-3-flash", "")), 7L, "127.0.0.1");

        ConfiguracionGemini guardada = capturarGuardada();
        assertThat(guardada.apiKey()).isEqualTo("clave-nueva-abcd");
        assertThat(guardada.modelos()).containsExactly("gemini-3-flash", "gemini-2.5-flash");
        assertThat(guardada.actualizadoPor()).isEqualTo(7L);
        verify(gemini).reiniciarRotacion();
    }

    @Test
    void sinClaveNuevaConservaLaGuardada() {
        when(repositorio.obtener()).thenReturn(Optional.of(
                new ConfiguracionGemini("clave-guardada", false, List.of("gemini-2.5-flash"), 1L, null)));

        useCase.actualizar(new ConfigurarGeminiUseCase.Cambios("", false, List.of("gemini-3-flash")), 7L, null);

        assertThat(capturarGuardada().apiKey()).isEqualTo("clave-guardada");
    }

    @Test
    void eliminarClaveVuelveALaDelEntorno() {
        when(repositorio.obtener()).thenReturn(Optional.of(
                new ConfiguracionGemini("clave-guardada", false, List.of("gemini-2.5-flash"), 1L, null)));

        useCase.actualizar(new ConfigurarGeminiUseCase.Cambios(null, true, List.of("gemini-2.5-flash")), 7L, null);

        assertThat(capturarGuardada().apiKey()).isNull();
    }

    @Test
    void rechazaClaveNuevaYEliminarALaVez() {
        assertThatThrownBy(() -> useCase.actualizar(
                new ConfigurarGeminiUseCase.Cambios("clave", true, List.of("gemini-2.5-flash")), 7L, null))
                .isInstanceOf(ValidacionDeNegocioException.class);
        verify(repositorio, never()).guardar(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rechazaUnaListaDeModelosVacia() {
        assertThatThrownBy(() -> useCase.actualizar(
                new ConfigurarGeminiUseCase.Cambios(null, false, Arrays.asList(" ", null)), 7L, null))
                .isInstanceOf(ValidacionDeNegocioException.class);
    }

    @Test
    void laAuditoriaNuncaIncluyeLaClaveCompleta() {
        when(repositorio.obtener()).thenReturn(Optional.empty());

        useCase.actualizar(new ConfigurarGeminiUseCase.Cambios("AIzaSySecretoMuyLargo1234", false,
                List.of("gemini-3-flash")), 7L, "127.0.0.1");

        ArgumentCaptor<OperacionAuditadaEvent> evento = ArgumentCaptor.forClass(OperacionAuditadaEvent.class);
        verify(eventPublisher).publishEvent(evento.capture());
        assertThat(evento.getValue().accion()).isEqualTo("CONFIGURACION_GEMINI_ACTUALIZADA");
        assertThat(evento.getValue().valoresNuevos()).contains("****1234").doesNotContain("AIzaSySecreto");
        assertThat(evento.getValue().valoresPrevios()).contains("****9999").doesNotContain("clave-entorno");
    }

    private ConfiguracionGemini capturarGuardada() {
        ArgumentCaptor<ConfiguracionGemini> captor = ArgumentCaptor.forClass(ConfiguracionGemini.class);
        verify(repositorio).guardar(captor.capture());
        return captor.getValue();
    }
}

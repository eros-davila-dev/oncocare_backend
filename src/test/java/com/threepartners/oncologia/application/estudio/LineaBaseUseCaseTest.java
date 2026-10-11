package com.threepartners.oncologia.application.estudio;

import com.threepartners.oncologia.domain.estudio.Fase;
import com.threepartners.oncologia.domain.estudio.LineaBaseRepositoryPort;
import com.threepartners.oncologia.domain.estudio.LineaBaseSesion;
import com.threepartners.oncologia.domain.shared.exception.ValidacionDeNegocioException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class LineaBaseUseCaseTest {

    private final LineaBaseRepositoryPort repositorio = mock(LineaBaseRepositoryPort.class);
    private final LineaBaseUseCase useCase = new LineaBaseUseCase(repositorio, mock(ApplicationEventPublisher.class),
            Clock.fixed(Instant.parse("2026-10-11T12:00:00Z"), ZoneOffset.UTC));

    @Test
    @SuppressWarnings("unchecked")
    void guardaSoloElPretestYDescartaElPostest() {
        var resultado = useCase.cargar(List.of(
                fila("1", "Pretest", "05/08/2026", "9.65", "41.67", "75"),
                fila("1", "Pretest", "03/08/2026", "9.44", "44,44", "77.78"),
                fila("2", "Postest", "02/09/2026", "3.44", "0", "66.67")), 7L, "127.0.0.1");

        assertThat(resultado.sesionesCargadas()).isEqualTo(2);
        assertThat(resultado.filasPostestIgnoradas()).isEqualTo(1);
        ArgumentCaptor<List<LineaBaseSesion>> guardadas = ArgumentCaptor.forClass(List.class);
        verify(repositorio).reemplazar(eq(Fase.PRETEST), guardadas.capture(), eq(7L));
        assertThat(guardadas.getValue()).extracting(LineaBaseSesion::fecha)
                .containsExactly(LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 5));
        assertThat(guardadas.getValue().get(0).taPct()).isEqualTo(44.44);
        assertThat(guardadas.getValue().get(0).numero()).isEqualTo(1);
    }

    @Test
    void unArchivoSoloConPostestNoCargaNada() {
        assertThatThrownBy(() -> useCase.cargar(List.of(fila("2", "Postest", "02/09/2026", "3.4", "0", "66")), 7L, "ip"))
                .isInstanceOf(ValidacionDeNegocioException.class);
        verify(repositorio, never()).reemplazar(any(), any(), any());
    }

    @Test
    void rechazaPorcentajesFueraDeRango() {
        assertThatThrownBy(() -> useCase.cargar(List.of(fila("1", "Pretest", "03/08/2026", "9", "140", "70")), 7L, "ip"))
                .isInstanceOf(ValidacionDeNegocioException.class)
                .hasMessageContaining("fuera de rango");
    }

    @Test
    @SuppressWarnings("unchecked")
    void elPostestImportadoQuedaRotuladoYNoMezclaElPretest() {
        var resultado = useCase.importarPostest(List.of(
                fila("1", "Pretest", "03/08/2026", "9.44", "44.44", "77.78"),
                fila("2", "Postest", "02/09/2026", "3.44", "0", "66.67")), 7L, "ip");

        assertThat(resultado.sesionesCargadas()).isEqualTo(1);
        ArgumentCaptor<List<LineaBaseSesion>> guardadas = ArgumentCaptor.forClass(List.class);
        verify(repositorio).reemplazar(eq(Fase.POSTEST), guardadas.capture(), eq(7L));
        assertThat(guardadas.getValue()).singleElement().satisfies(s -> {
            assertThat(s.fase()).isEqualTo(Fase.POSTEST);
            assertThat(s.fuente()).contains("servidor de la fundación");
        });
    }

    private static Map<String, String> fila(String grupo, String etapa, String fecha, String tpr, String ta, String nca) {
        return Map.of("sesion", "1", "grupo", grupo, "etapa", etapa, "fecha", fecha, "tpr", tpr, "ta", ta, "nca", nca);
    }
}

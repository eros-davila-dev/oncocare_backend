package com.threepartners.oncologia.application.estudio;

import com.threepartners.oncologia.domain.estudio.EstadoFase;
import com.threepartners.oncologia.domain.estudio.Fase;
import com.threepartners.oncologia.domain.estudio.FaseEstudio;
import com.threepartners.oncologia.domain.estudio.FaseEstudioRepositoryPort;
import com.threepartners.oncologia.domain.shared.exception.ValidacionDeNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GestionarFasesEstudioUseCaseTest {

    @Mock
    private FaseEstudioRepositoryPort repositorio;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private GestionarFasesEstudioUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GestionarFasesEstudioUseCase(repositorio, eventPublisher);
    }

    private FaseEstudio pretest(LocalDate inicio, LocalDate fin) {
        return FaseEstudio.builder().fase(Fase.PRETEST).estado(EstadoFase.ABIERTA).fechaInicio(inicio).fechaFin(fin).build();
    }

    @Test
    void configuraElPostestCuandoEmpiezaDespuesDelPretest() {
        when(repositorio.buscarPorFase(Fase.POSTEST)).thenReturn(Optional.empty());
        when(repositorio.buscarPorFase(Fase.PRETEST))
                .thenReturn(Optional.of(pretest(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30))));
        when(repositorio.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        FaseEstudio postest = useCase.configurar(Fase.POSTEST, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), 1L, "ip");

        assertThat(postest.getEstado()).isEqualTo(EstadoFase.ABIERTA);
        assertThat(postest.getFechaInicio()).isEqualTo(LocalDate.of(2026, 10, 1));
    }

    @Test
    void rechazaFasesQueSeSolapan() {
        when(repositorio.buscarPorFase(Fase.POSTEST)).thenReturn(Optional.empty());
        when(repositorio.buscarPorFase(Fase.PRETEST))
                .thenReturn(Optional.of(pretest(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 5))));

        assertThatThrownBy(() -> useCase.configurar(Fase.POSTEST, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), 1L, "ip"))
                .isInstanceOf(ValidacionDeNegocioException.class)
                .hasMessageContaining("pretest debe terminar antes");
        verify(repositorio, never()).guardar(any());
    }
}

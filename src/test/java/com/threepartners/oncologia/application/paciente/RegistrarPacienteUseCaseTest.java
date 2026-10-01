package com.threepartners.oncologia.application.paciente;

import com.threepartners.oncologia.domain.paciente.Paciente;
import com.threepartners.oncologia.domain.paciente.PacienteRepositoryPort;
import com.threepartners.oncologia.domain.shared.exception.ConflictoDeNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrarPacienteUseCaseTest {

    @Mock
    private PacienteRepositoryPort pacienteRepositoryPort;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private RegistrarPacienteUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new RegistrarPacienteUseCase(pacienteRepositoryPort, eventPublisher);
    }

    @Test
    void registraElPacienteCuandoElDocumentoEsUnico() {
        Paciente paciente = Paciente.builder().documentoIdentidad("12345678").nombres("Ana").apellidos("Perez").build();
        when(pacienteRepositoryPort.existePorDocumento("12345678")).thenReturn(false);
        when(pacienteRepositoryPort.guardar(any(Paciente.class))).thenAnswer(inv -> inv.getArgument(0));

        Paciente resultado = useCase.ejecutar(paciente, 1L, "127.0.0.1");

        assertThat(resultado.isActivo()).isTrue();
        assertThat(resultado.getFechaRegistro()).isNotNull();
    }

    @Test
    void rechazaElRegistroCuandoElDocumentoYaExiste() {
        Paciente paciente = Paciente.builder().documentoIdentidad("12345678").build();
        when(pacienteRepositoryPort.existePorDocumento("12345678")).thenReturn(true);

        assertThatThrownBy(() -> useCase.ejecutar(paciente, 1L, "127.0.0.1"))
                .isInstanceOf(ConflictoDeNegocioException.class);
    }
}

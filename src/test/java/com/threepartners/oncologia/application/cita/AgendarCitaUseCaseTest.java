package com.threepartners.oncologia.application.cita;

import com.threepartners.oncologia.domain.cita.Cita;
import com.threepartners.oncologia.domain.cita.CitaRepositoryPort;
import com.threepartners.oncologia.domain.cita.EstadoCita;
import com.threepartners.oncologia.domain.paciente.Paciente;
import com.threepartners.oncologia.domain.paciente.PacienteRepositoryPort;
import com.threepartners.oncologia.domain.shared.exception.ConflictoDeNegocioException;
import com.threepartners.oncologia.domain.shared.exception.RecursoNoEncontradoException;
import com.threepartners.oncologia.domain.shared.exception.ValidacionDeNegocioException;
import com.threepartners.oncologia.domain.usuario.Especialidad;
import com.threepartners.oncologia.domain.usuario.Rol;
import com.threepartners.oncologia.domain.usuario.Usuario;
import com.threepartners.oncologia.domain.usuario.UsuarioRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgendarCitaUseCaseTest {

    @Mock
    private CitaRepositoryPort citaRepositoryPort;
    @Mock
    private PacienteRepositoryPort pacienteRepositoryPort;
    @Mock
    private UsuarioRepositoryPort usuarioRepositoryPort;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private AgendarCitaUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new AgendarCitaUseCase(citaRepositoryPort, pacienteRepositoryPort, usuarioRepositoryPort, eventPublisher);
    }

    private Cita citaDePrueba() {
        return Cita.builder()
                .pacienteId(1L)
                .medicoId(2L)
                .fecha(LocalDate.now().plusDays(1))
                .hora(LocalTime.of(10, 0))
                .tipoConsulta("Control")
                .build();
    }

    private Usuario medicoValido() {
        return Usuario.builder().id(2L).rol(Rol.MEDICO).especialidad(Especialidad.ONCOLOGIA_CLINICA).build();
    }

    @Test
    void agendaLaCitaCuandoNoHaySolapamiento() {
        when(pacienteRepositoryPort.buscarPorId(1L)).thenReturn(java.util.Optional.of(Paciente.builder().id(1L).build()));
        when(usuarioRepositoryPort.buscarPorId(2L)).thenReturn(java.util.Optional.of(medicoValido()));
        when(citaRepositoryPort.existeSolapamiento(2L, LocalDate.now().plusDays(1), LocalTime.of(10, 0), null)).thenReturn(false);
        when(citaRepositoryPort.guardar(any(Cita.class))).thenAnswer(inv -> inv.getArgument(0));

        Cita resultado = useCase.ejecutar(citaDePrueba(), 99L, Rol.RECEPCIONISTA, "127.0.0.1");

        assertThat(resultado.getEstado()).isEqualTo(EstadoCita.PROGRAMADA);
    }

    @Test
    void rechazaLaCitaCuandoElMedicoYaTieneUnaEnEseHorario() {
        when(pacienteRepositoryPort.buscarPorId(1L)).thenReturn(java.util.Optional.of(Paciente.builder().id(1L).build()));
        when(usuarioRepositoryPort.buscarPorId(2L)).thenReturn(java.util.Optional.of(medicoValido()));
        when(citaRepositoryPort.existeSolapamiento(2L, LocalDate.now().plusDays(1), LocalTime.of(10, 0), null)).thenReturn(true);

        assertThatThrownBy(() -> useCase.ejecutar(citaDePrueba(), 99L, Rol.RECEPCIONISTA, "127.0.0.1"))
                .isInstanceOf(ConflictoDeNegocioException.class);
    }

    @Test
    void rechazaLaCitaCuandoElPacienteNoExiste() {
        when(pacienteRepositoryPort.buscarPorId(1L)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> useCase.ejecutar(citaDePrueba(), 99L, Rol.RECEPCIONISTA, "127.0.0.1"))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void rechazaLaCitaCuandoElUsuarioAsignadoNoEsMedico() {
        when(pacienteRepositoryPort.buscarPorId(1L)).thenReturn(java.util.Optional.of(Paciente.builder().id(1L).build()));
        when(usuarioRepositoryPort.buscarPorId(2L)).thenReturn(java.util.Optional.of(
                Usuario.builder().id(2L).rol(Rol.RECEPCIONISTA).build()));

        assertThatThrownBy(() -> useCase.ejecutar(citaDePrueba(), 99L, Rol.RECEPCIONISTA, "127.0.0.1"))
                .isInstanceOf(ValidacionDeNegocioException.class);
    }
}

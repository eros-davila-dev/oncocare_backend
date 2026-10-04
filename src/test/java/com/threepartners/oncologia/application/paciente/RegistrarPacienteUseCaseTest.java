package com.threepartners.oncologia.application.paciente;

import com.threepartners.oncologia.application.estudio.CerrarMedicionRegistroService;
import com.threepartners.oncologia.domain.estudio.TipoMedicion;
import com.threepartners.oncologia.domain.paciente.Paciente;
import com.threepartners.oncologia.domain.paciente.PacienteRepositoryPort;
import com.threepartners.oncologia.domain.shared.exception.ConflictoDeNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrarPacienteUseCaseTest {

    @Mock
    private PacienteRepositoryPort pacienteRepositoryPort;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private CerrarMedicionRegistroService cerrarMedicionRegistroService;

    private final Clock clock = Clock.fixed(Instant.parse("2026-10-01T15:00:00Z"), ZoneOffset.UTC);

    private RegistrarPacienteUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new RegistrarPacienteUseCase(pacienteRepositoryPort, eventPublisher, cerrarMedicionRegistroService, clock);
    }

    @Test
    void registraElPacienteCuandoElDocumentoEsUnico() {
        Paciente paciente = Paciente.builder().documentoIdentidad("12345678").telefono("904049494").nombres("Ana").apellidos("Perez")
                .email("ana@correo.pe").contactoEmergenciaEmail("hija@correo.pe").build();
        when(pacienteRepositoryPort.existePorDocumento("12345678")).thenReturn(false);
        when(pacienteRepositoryPort.guardar(any(Paciente.class))).thenAnswer(inv -> {
            Paciente p = inv.getArgument(0);
            p.setId(10L);
            return p;
        });

        Paciente resultado = useCase.ejecutar(paciente, 55L, 1L, "127.0.0.1");

        assertThat(resultado.isActivo()).isTrue();
        assertThat(resultado.getFechaRegistro()).isEqualTo(clock.instant());
        verify(cerrarMedicionRegistroService).cerrar(55L, TipoMedicion.REGISTRO_PACIENTE, 1L, 10L, 10L);
    }

    @Test
    void rechazaElRegistroCuandoElDocumentoYaExiste() {
        Paciente paciente = Paciente.builder().documentoIdentidad("12345678").telefono("904049494")
                .email("ana@correo.pe").contactoEmergenciaEmail("hija@correo.pe").build();
        when(pacienteRepositoryPort.existePorDocumento("12345678")).thenReturn(true);

        assertThatThrownBy(() -> useCase.ejecutar(paciente, null, 1L, "127.0.0.1"))
                .isInstanceOf(ConflictoDeNegocioException.class);
    }

    @Test
    void exigeElCorreoDelPacienteYElDelReferido() {
        Paciente sinCorreo = Paciente.builder().documentoIdentidad("12345678").telefono("904049494").contactoEmergenciaEmail("hija@correo.pe").build();
        Paciente sinReferido = Paciente.builder().documentoIdentidad("12345678").telefono("904049494").email("ana@correo.pe").build();
        Paciente mismoCorreo = Paciente.builder().documentoIdentidad("12345678").telefono("904049494")
                .email("ana@correo.pe").contactoEmergenciaEmail("ANA@correo.pe").build();

        assertThatThrownBy(() -> useCase.ejecutar(sinCorreo, null, 1L, null))
                .isInstanceOf(com.threepartners.oncologia.domain.shared.exception.ValidacionDeNegocioException.class)
                .hasMessageContaining("paciente");
        assertThatThrownBy(() -> useCase.ejecutar(sinReferido, null, 1L, null))
                .hasMessageContaining("referido");
        assertThatThrownBy(() -> useCase.ejecutar(mismoCorreo, null, 1L, null))
                .hasMessageContaining("distinto");
        org.mockito.Mockito.verify(pacienteRepositoryPort, org.mockito.Mockito.never()).guardar(any());
    }

    @Test
    void exigeElTelefonoDelPacienteYQueSeaDistintoAlDelReferido() {
        Paciente sinTelefono = Paciente.builder().documentoIdentidad("12345678")
                .email("ana@correo.pe").contactoEmergenciaEmail("hija@correo.pe").build();
        Paciente mismoTelefono = Paciente.builder().documentoIdentidad("12345678").telefono("+51 904 049 494")
                .contactoEmergenciaTelefono("904049494").email("ana@correo.pe").contactoEmergenciaEmail("hija@correo.pe").build();

        assertThatThrownBy(() -> useCase.ejecutar(sinTelefono, null, 1L, null)).hasMessageContaining("telefono del paciente");
        assertThatThrownBy(() -> useCase.ejecutar(mismoTelefono, null, 1L, null)).hasMessageContaining("distinto al del paciente");
    }
}

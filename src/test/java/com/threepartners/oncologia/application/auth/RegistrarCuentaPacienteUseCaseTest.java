package com.threepartners.oncologia.application.auth;

import com.threepartners.oncologia.config.FrontendProperties;
import com.threepartners.oncologia.config.RegistroProperties;
import com.threepartners.oncologia.domain.shared.exception.RegistroNoDisponibleException;
import com.threepartners.oncologia.domain.usuario.GeneradorTokenPort;
import com.threepartners.oncologia.domain.usuario.PasswordEncoderPort;
import com.threepartners.oncologia.domain.usuario.TokenAccionCuentaRepositoryPort;
import com.threepartners.oncologia.domain.usuario.Usuario;
import com.threepartners.oncologia.domain.usuario.UsuarioRepositoryPort;
import com.threepartners.oncologia.infrastructure.out.notification.N8nNotificationAdapter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrarCuentaPacienteUseCaseTest {

    @Mock
    private UsuarioRepositoryPort usuarioRepositoryPort;
    @Mock
    private PasswordEncoderPort passwordEncoderPort;
    @Mock
    private TokenAccionCuentaRepositoryPort tokenAccionCuentaRepositoryPort;
    @Mock
    private GeneradorTokenPort generadorTokenPort;
    @Mock
    private N8nNotificationAdapter n8nNotificationAdapter;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private final FrontendProperties frontendProperties = new FrontendProperties("http://localhost:4200");

    private RegistrarCuentaPacienteUseCase useCase(boolean pacientesHabilitado) {
        return new RegistrarCuentaPacienteUseCase(
                usuarioRepositoryPort, passwordEncoderPort, tokenAccionCuentaRepositoryPort, generadorTokenPort,
                n8nNotificationAdapter, frontendProperties, new RegistroProperties(pacientesHabilitado), eventPublisher);
    }

    @Test
    void rechazaElRegistroSiElInterruptorEstaDeshabilitadoSinTocarLaBaseDeDatos() {
        RegistrarCuentaPacienteUseCase useCase = useCase(false);

        assertThatThrownBy(() -> useCase.ejecutar("Ana", "ana@example.com", "Clave1234", "127.0.0.1"))
                .isInstanceOf(RegistroNoDisponibleException.class);

        verifyNoInteractions(usuarioRepositoryPort, tokenAccionCuentaRepositoryPort, n8nNotificationAdapter, eventPublisher);
    }

    @Test
    void creaLaCuentaYDisparaElTokenDeVerificacionCuandoElInterruptorEstaHabilitado() {
        RegistrarCuentaPacienteUseCase useCase = useCase(true);
        when(usuarioRepositoryPort.existePorEmail("ana@example.com")).thenReturn(false);
        when(passwordEncoderPort.encriptar("Clave1234")).thenReturn("hash");
        when(usuarioRepositoryPort.guardar(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(generadorTokenPort.generarToken()).thenReturn("token-plano");
        when(generadorTokenPort.hash(anyString())).thenReturn("token-hash");

        useCase.ejecutar("Ana", "ana@example.com", "Clave1234", "127.0.0.1");

        verify(usuarioRepositoryPort).guardar(any(Usuario.class));
        verify(tokenAccionCuentaRepositoryPort).guardar(any());
        verify(n8nNotificationAdapter).dispararWorkflow(anyString(), any());
        verify(eventPublisher).publishEvent(any(Object.class));
    }

    @Test
    void noCreaUnaSegundaCuentaSiElCorreoYaExisteYNoLoRevela() {
        RegistrarCuentaPacienteUseCase useCase = useCase(true);
        when(usuarioRepositoryPort.existePorEmail("ana@example.com")).thenReturn(true);

        useCase.ejecutar("Ana", "ana@example.com", "Clave1234", "127.0.0.1");

        verify(usuarioRepositoryPort, never()).guardar(any(Usuario.class));
        verifyNoInteractions(n8nNotificationAdapter, eventPublisher);
    }
}

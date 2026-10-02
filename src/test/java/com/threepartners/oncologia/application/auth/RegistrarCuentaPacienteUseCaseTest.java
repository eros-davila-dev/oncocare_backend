package com.threepartners.oncologia.application.auth;

import com.threepartners.oncologia.config.FrontendProperties;
import com.threepartners.oncologia.config.RegistroProperties;
import com.threepartners.oncologia.domain.shared.exception.RegistroNoDisponibleException;
import com.threepartners.oncologia.domain.usuario.GeneradorTokenPort;
import com.threepartners.oncologia.domain.usuario.PasswordEncoderPort;
import com.threepartners.oncologia.domain.usuario.TokenAccionCuentaRepositoryPort;
import com.threepartners.oncologia.domain.usuario.Usuario;
import com.threepartners.oncologia.domain.usuario.UsuarioRepositoryPort;
import com.threepartners.oncologia.domain.notificacion.NotificadorCorreoPort;
import com.threepartners.oncologia.domain.notificacion.TipoCorreo;
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
    private NotificadorCorreoPort notificadorCorreoPort;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private final FrontendProperties frontendProperties = new FrontendProperties("http://localhost:4200");

    private RegistrarCuentaPacienteUseCase useCase(boolean pacientesHabilitado) {
        return new RegistrarCuentaPacienteUseCase(
                usuarioRepositoryPort, passwordEncoderPort, tokenAccionCuentaRepositoryPort, generadorTokenPort,
                notificadorCorreoPort, frontendProperties, new RegistroProperties(pacientesHabilitado), eventPublisher);
    }

    @Test
    void rechazaElRegistroSiElInterruptorEstaDeshabilitadoSinTocarLaBaseDeDatos() {
        RegistrarCuentaPacienteUseCase useCase = useCase(false);

        assertThatThrownBy(() -> useCase.ejecutar("Ana", "ana@example.com", "Clave1234", "127.0.0.1"))
                .isInstanceOf(RegistroNoDisponibleException.class);

        verifyNoInteractions(usuarioRepositoryPort, tokenAccionCuentaRepositoryPort, notificadorCorreoPort, eventPublisher);
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
        verify(notificadorCorreoPort).enviar(TipoCorreo.VERIFICACION_EMAIL, "ana@example.com", "Ana",
                "http://localhost:4200/auth/verificar-email?token=token-plano");
        verify(eventPublisher).publishEvent(any(Object.class));
    }

    @Test
    void noCreaUnaSegundaCuentaSiElCorreoYaExisteYNoLoRevela() {
        RegistrarCuentaPacienteUseCase useCase = useCase(true);
        when(usuarioRepositoryPort.existePorEmail("ana@example.com")).thenReturn(true);

        useCase.ejecutar("Ana", "ana@example.com", "Clave1234", "127.0.0.1");

        verify(usuarioRepositoryPort, never()).guardar(any(Usuario.class));
        verifyNoInteractions(notificadorCorreoPort, eventPublisher);
    }
}

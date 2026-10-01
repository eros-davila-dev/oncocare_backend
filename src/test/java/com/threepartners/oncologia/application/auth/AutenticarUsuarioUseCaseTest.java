package com.threepartners.oncologia.application.auth;

import com.threepartners.oncologia.domain.shared.exception.CredencialesInvalidasException;
import com.threepartners.oncologia.domain.usuario.GeneradorTokenPort;
import com.threepartners.oncologia.domain.usuario.PasswordEncoderPort;
import com.threepartners.oncologia.domain.usuario.Rol;
import com.threepartners.oncologia.domain.usuario.SesionRefreshTokenRepositoryPort;
import com.threepartners.oncologia.domain.usuario.TokenGenerado;
import com.threepartners.oncologia.domain.usuario.TokenProviderPort;
import com.threepartners.oncologia.domain.usuario.Usuario;
import com.threepartners.oncologia.domain.usuario.UsuarioRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AutenticarUsuarioUseCaseTest {

    @Mock
    private UsuarioRepositoryPort usuarioRepositoryPort;
    @Mock
    private PasswordEncoderPort passwordEncoderPort;
    @Mock
    private TokenProviderPort tokenProviderPort;
    @Mock
    private SesionRefreshTokenRepositoryPort sesionRefreshTokenRepositoryPort;
    @Mock
    private GeneradorTokenPort generadorTokenPort;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private AutenticarUsuarioUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new AutenticarUsuarioUseCase(usuarioRepositoryPort, passwordEncoderPort, tokenProviderPort,
                sesionRefreshTokenRepositoryPort, generadorTokenPort, eventPublisher);
    }

    @Test
    void autenticaCorrectamenteConCredencialesValidas() {
        Usuario usuario = Usuario.builder().id(1L).email("admin@threepartners.org").passwordHash("hash").rol(Rol.ADMIN).activo(true).build();
        when(usuarioRepositoryPort.buscarPorEmail("admin@threepartners.org")).thenReturn(Optional.of(usuario));
        when(passwordEncoderPort.coincide("Admin123!", "hash")).thenReturn(true);
        when(tokenProviderPort.generarAccessToken(usuario)).thenReturn("access-token");
        when(tokenProviderPort.generarRefreshToken(usuario))
                .thenReturn(new TokenGenerado("refresh-token", "jti-de-prueba", Instant.now().plusSeconds(3600)));

        LoginResultado resultado = useCase.ejecutar("admin@threepartners.org", "Admin123!", "127.0.0.1");

        assertThat(resultado.accessToken()).isEqualTo("access-token");
        assertThat(resultado.refreshToken()).isEqualTo("refresh-token");
    }

    @Test
    void rechazaCuandoLaContrasenaNoCoincide() {
        Usuario usuario = Usuario.builder().id(1L).email("admin@threepartners.org").passwordHash("hash").rol(Rol.ADMIN).activo(true).build();
        when(usuarioRepositoryPort.buscarPorEmail("admin@threepartners.org")).thenReturn(Optional.of(usuario));
        when(passwordEncoderPort.coincide("incorrecta", "hash")).thenReturn(false);

        assertThatThrownBy(() -> useCase.ejecutar("admin@threepartners.org", "incorrecta", "127.0.0.1"))
                .isInstanceOf(CredencialesInvalidasException.class);
    }

    @Test
    void rechazaCuandoElUsuarioNoExiste() {
        when(usuarioRepositoryPort.buscarPorEmail("desconocido@threepartners.org")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.ejecutar("desconocido@threepartners.org", "cualquiera", "127.0.0.1"))
                .isInstanceOf(CredencialesInvalidasException.class);
    }

    @Test
    void rechazaCuandoElUsuarioEstaInactivo() {
        Usuario usuario = Usuario.builder().id(1L).email("inactivo@threepartners.org").passwordHash("hash").rol(Rol.MEDICO).activo(false).build();
        when(usuarioRepositoryPort.buscarPorEmail("inactivo@threepartners.org")).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> useCase.ejecutar("inactivo@threepartners.org", "cualquiera", "127.0.0.1"))
                .isInstanceOf(CredencialesInvalidasException.class);
    }
}

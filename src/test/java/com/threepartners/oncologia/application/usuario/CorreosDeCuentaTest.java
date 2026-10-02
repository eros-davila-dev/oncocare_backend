package com.threepartners.oncologia.application.usuario;

import com.threepartners.oncologia.application.auth.SolicitarRecuperacionPasswordUseCase;
import com.threepartners.oncologia.config.FrontendProperties;
import com.threepartners.oncologia.domain.notificacion.NotificadorCorreoPort;
import com.threepartners.oncologia.domain.notificacion.TipoCorreo;
import com.threepartners.oncologia.domain.usuario.GeneradorTokenPort;
import com.threepartners.oncologia.domain.usuario.PasswordEncoderPort;
import com.threepartners.oncologia.domain.usuario.Rol;
import com.threepartners.oncologia.domain.usuario.TipoTokenCuenta;
import com.threepartners.oncologia.domain.usuario.TokenAccionCuenta;
import com.threepartners.oncologia.domain.usuario.TokenAccionCuentaRepositoryPort;
import com.threepartners.oncologia.domain.usuario.Usuario;
import com.threepartners.oncologia.domain.usuario.UsuarioRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Los enlaces de los correos llevan a la aplicacion correcta: el paciente al
 * portal, el personal a la intranet (donde inicia sesion).
 */
class CorreosDeCuentaTest {

    private final UsuarioRepositoryPort usuarios = mock(UsuarioRepositoryPort.class);
    private final TokenAccionCuentaRepositoryPort tokens = mock(TokenAccionCuentaRepositoryPort.class);
    private final GeneradorTokenPort generador = mock(GeneradorTokenPort.class);
    private final NotificadorCorreoPort correo = mock(NotificadorCorreoPort.class);
    private final FrontendProperties frontend = new FrontendProperties("https://portal.pe", "https://intranet.pe");

    @BeforeEach
    void tokenFijo() {
        when(generador.generarToken()).thenReturn("tk");
        when(generador.hash("tk")).thenReturn("hash-tk");
    }

    @Test
    void elPersonalRestableceEnLaIntranetYElPacienteEnElPortal() {
        var recuperar = new SolicitarRecuperacionPasswordUseCase(usuarios, tokens, generador, correo, frontend);
        when(usuarios.buscarPorEmail("medico@fundacion.pe")).thenReturn(Optional.of(usuario(Rol.MEDICO, "medico@fundacion.pe")));
        when(usuarios.buscarPorEmail("ana@correo.pe")).thenReturn(Optional.of(usuario(Rol.PACIENTE, "ana@correo.pe")));

        recuperar.ejecutar("Medico@Fundacion.pe ");
        recuperar.ejecutar("ana@correo.pe");

        verify(correo).enviar(TipoCorreo.RESTABLECER_PASSWORD, "medico@fundacion.pe", "Luis Rojas",
                "https://intranet.pe/auth/restablecer-password?token=tk");
        verify(correo).enviar(TipoCorreo.RESTABLECER_PASSWORD, "ana@correo.pe", "Luis Rojas",
                "https://portal.pe/auth/restablecer-password?token=tk");
    }

    @Test
    void unCorreoDesconocidoNoRecibeNadaNiRevelaSiExiste() {
        var recuperar = new SolicitarRecuperacionPasswordUseCase(usuarios, tokens, generador, correo, frontend);
        when(usuarios.buscarPorEmail(anyString())).thenReturn(Optional.empty());

        recuperar.ejecutar("nadie@correo.pe");

        verifyNoInteractions(correo);
    }

    @Test
    void crearUnUsuarioLeEnviaLaBienvenidaConUnEnlaceDeUnSoloUsoDe72Horas() {
        PasswordEncoderPort encoder = mock(PasswordEncoderPort.class);
        when(encoder.encriptar(anyString())).thenReturn("hash");
        when(usuarios.guardar(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setId(9L);
            return u;
        });
        var crear = new CrearUsuarioUseCase(usuarios, encoder, tokens, generador, correo, frontend);

        crear.ejecutar(usuario(Rol.RECEPCIONISTA, "recepcion@fundacion.pe"), "Inicial-12345");

        ArgumentCaptor<TokenAccionCuenta> token = ArgumentCaptor.forClass(TokenAccionCuenta.class);
        verify(tokens).guardar(token.capture());
        assertThat(token.getValue().getTipo()).isEqualTo(TipoTokenCuenta.RESET_PASSWORD);
        assertThat(token.getValue().getTokenHash()).isEqualTo("hash-tk");
        assertThat(Duration.between(Instant.now(), token.getValue().getExpiraEn()).toHours())
                .isCloseTo(CrearUsuarioUseCase.VIGENCIA_ENLACE_BIENVENIDA.toHours(), within(1L));
        verify(correo).enviar(TipoCorreo.BIENVENIDA_USUARIO, "recepcion@fundacion.pe", "Luis Rojas",
                "https://intranet.pe/auth/restablecer-password?token=tk");
    }

    private static Usuario usuario(Rol rol, String email) {
        return Usuario.builder().id(1L).nombres("Luis Rojas").email(email).rol(rol).activo(true).build();
    }
}

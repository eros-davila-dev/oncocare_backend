package com.threepartners.oncologia.application.usuario;

import com.threepartners.oncologia.config.FrontendProperties;
import com.threepartners.oncologia.domain.notificacion.NotificadorCorreoPort;
import com.threepartners.oncologia.domain.notificacion.TipoCorreo;
import com.threepartners.oncologia.domain.shared.exception.ConflictoDeNegocioException;
import com.threepartners.oncologia.domain.usuario.EstadoCuenta;
import com.threepartners.oncologia.domain.usuario.GeneradorTokenPort;
import com.threepartners.oncologia.domain.usuario.PasswordEncoderPort;
import com.threepartners.oncologia.domain.usuario.TipoTokenCuenta;
import com.threepartners.oncologia.domain.usuario.TokenAccionCuenta;
import com.threepartners.oncologia.domain.usuario.TokenAccionCuentaRepositoryPort;
import com.threepartners.oncologia.domain.usuario.Usuario;
import com.threepartners.oncologia.domain.usuario.UsuarioRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/**
 * Alta de un usuario por el administrador. Ademas de guardarlo, le envia un
 * correo de bienvenida con un enlace de un solo uso para que elija su propia
 * contrasena: asi la contrasena inicial que fijo el administrador no tiene
 * por que viajar por chat ni quedar en papel.
 */
@Service
@RequiredArgsConstructor
public class CrearUsuarioUseCase {

    static final Duration VIGENCIA_ENLACE_BIENVENIDA = Duration.ofHours(72);

    private final UsuarioRepositoryPort usuarioRepositoryPort;
    private final PasswordEncoderPort passwordEncoderPort;
    private final TokenAccionCuentaRepositoryPort tokenAccionCuentaRepositoryPort;
    private final GeneradorTokenPort generadorTokenPort;
    private final NotificadorCorreoPort notificadorCorreoPort;
    private final FrontendProperties frontendProperties;

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public Usuario ejecutar(Usuario nuevoUsuario, String passwordEnTextoPlano) {
        if (usuarioRepositoryPort.existePorEmail(nuevoUsuario.getEmail())) {
            throw new ConflictoDeNegocioException("Ya existe un usuario registrado con ese correo electronico");
        }

        nuevoUsuario.setPasswordHash(passwordEncoderPort.encriptar(passwordEnTextoPlano));
        nuevoUsuario.setActivo(true);
        nuevoUsuario.setFechaCreacion(Instant.now());
        nuevoUsuario.setEstadoCuenta(EstadoCuenta.ACTIVA);
        Usuario guardado = usuarioRepositoryPort.guardar(nuevoUsuario);

        enviarBienvenida(guardado);
        return guardado;
    }

    private void enviarBienvenida(Usuario usuario) {
        String tokenPlano = generadorTokenPort.generarToken();
        Instant ahora = Instant.now();
        tokenAccionCuentaRepositoryPort.guardar(TokenAccionCuenta.builder()
                .usuarioId(usuario.getId())
                .tipo(TipoTokenCuenta.RESET_PASSWORD)
                .tokenHash(generadorTokenPort.hash(tokenPlano))
                .expiraEn(ahora.plus(VIGENCIA_ENLACE_BIENVENIDA))
                .creadoEn(ahora)
                .build());

        notificadorCorreoPort.enviar(TipoCorreo.BIENVENIDA_USUARIO, usuario.getEmail(), usuario.getNombres(),
                frontendProperties.urlDeAplicacion(usuario.getRol()) + "/auth/restablecer-password?token=" + tokenPlano);
    }
}

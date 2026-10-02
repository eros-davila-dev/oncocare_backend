package com.threepartners.oncologia.application.auth;

import com.threepartners.oncologia.config.FrontendProperties;
import com.threepartners.oncologia.domain.usuario.GeneradorTokenPort;
import com.threepartners.oncologia.domain.usuario.TipoTokenCuenta;
import com.threepartners.oncologia.domain.usuario.TokenAccionCuenta;
import com.threepartners.oncologia.domain.usuario.TokenAccionCuentaRepositoryPort;
import com.threepartners.oncologia.domain.usuario.Usuario;
import com.threepartners.oncologia.domain.usuario.UsuarioRepositoryPort;
import com.threepartners.oncologia.domain.notificacion.NotificadorCorreoPort;
import com.threepartners.oncologia.domain.notificacion.TipoCorreo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/**
 * Seccion 9: la respuesta al llamador es siempre la misma exista o no la
 * cuenta, para no permitir enumerar correos registrados. Solo si la cuenta
 * existe y esta activa se emite (y notifica) un token de restablecimiento.
 */
@Service
@RequiredArgsConstructor
public class SolicitarRecuperacionPasswordUseCase {

    private static final Duration VIGENCIA_TOKEN_RESET = Duration.ofHours(1);

    private final UsuarioRepositoryPort usuarioRepositoryPort;
    private final TokenAccionCuentaRepositoryPort tokenAccionCuentaRepositoryPort;
    private final GeneradorTokenPort generadorTokenPort;
    private final NotificadorCorreoPort notificadorCorreoPort;
    private final FrontendProperties frontendProperties;

    @Transactional
    public void ejecutar(String email) {
        String emailNormalizado = email.trim().toLowerCase();
        usuarioRepositoryPort.buscarPorEmail(emailNormalizado)
                .filter(Usuario::isActivo)
                .ifPresent(this::enviarTokenReset);
    }

    private void enviarTokenReset(Usuario cuenta) {
        tokenAccionCuentaRepositoryPort.invalidarPendientes(cuenta.getId(), TipoTokenCuenta.RESET_PASSWORD);

        String tokenPlano = generadorTokenPort.generarToken();
        tokenAccionCuentaRepositoryPort.guardar(TokenAccionCuenta.builder()
                .usuarioId(cuenta.getId())
                .tipo(TipoTokenCuenta.RESET_PASSWORD)
                .tokenHash(generadorTokenPort.hash(tokenPlano))
                .expiraEn(Instant.now().plus(VIGENCIA_TOKEN_RESET))
                .creadoEn(Instant.now())
                .build());

        // El paciente restablece en el portal; el personal, en la intranet.
        notificadorCorreoPort.enviar(TipoCorreo.RESTABLECER_PASSWORD, cuenta.getEmail(), cuenta.getNombres(),
                frontendProperties.urlDeAplicacion(cuenta.getRol()) + "/auth/restablecer-password?token=" + tokenPlano);
    }
}

package com.threepartners.oncologia.application.auth;

import com.threepartners.oncologia.domain.auditoria.ResultadoAuditoria;
import com.threepartners.oncologia.domain.auditoria.event.AccesoEvent;
import com.threepartners.oncologia.domain.shared.exception.CredencialesInvalidasException;
import com.threepartners.oncologia.domain.shared.exception.CuentaBloqueadaException;
import com.threepartners.oncologia.domain.shared.exception.CuentaNoVerificadaException;
import com.threepartners.oncologia.domain.usuario.EstadoCuenta;
import com.threepartners.oncologia.domain.usuario.GeneradorTokenPort;
import com.threepartners.oncologia.domain.usuario.PasswordEncoderPort;
import com.threepartners.oncologia.domain.usuario.SesionRefreshToken;
import com.threepartners.oncologia.domain.usuario.SesionRefreshTokenRepositoryPort;
import com.threepartners.oncologia.domain.usuario.TokenGenerado;
import com.threepartners.oncologia.domain.usuario.TokenProviderPort;
import com.threepartners.oncologia.domain.usuario.Usuario;
import com.threepartners.oncologia.domain.usuario.UsuarioRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class AutenticarUsuarioUseCase {

    private static final int MAX_INTENTOS_FALLIDOS = 5;
    private static final long BLOQUEO_MINUTOS = 15;

    private final UsuarioRepositoryPort usuarioRepositoryPort;
    private final PasswordEncoderPort passwordEncoderPort;
    private final TokenProviderPort tokenProviderPort;
    private final SesionRefreshTokenRepositoryPort sesionRefreshTokenRepositoryPort;
    private final GeneradorTokenPort generadorTokenPort;
    private final ApplicationEventPublisher eventPublisher;

    public LoginResultado ejecutar(String email, String password, String ipOrigen) {
        Usuario usuario = usuarioRepositoryPort.buscarPorEmail(email).orElse(null);

        if (usuario != null && usuario.estaBloqueada()) {
            eventPublisher.publishEvent(new AccesoEvent(usuario.getId(), email, ipOrigen, ResultadoAuditoria.FALLIDO));
            throw new CuentaBloqueadaException();
        }

        boolean credencialesValidas = usuario != null && usuario.isActivo()
                && passwordEncoderPort.coincide(password, usuario.getPasswordHash());

        if (!credencialesValidas) {
            if (usuario != null) {
                registrarIntentoFallido(usuario);
            }
            eventPublisher.publishEvent(new AccesoEvent(
                    usuario != null ? usuario.getId() : null, email, ipOrigen, ResultadoAuditoria.FALLIDO));
            throw new CredencialesInvalidasException();
        }

        if (usuario.getEstadoCuenta() == EstadoCuenta.PENDIENTE_VERIFICACION) {
            eventPublisher.publishEvent(new AccesoEvent(usuario.getId(), email, ipOrigen, ResultadoAuditoria.FALLIDO));
            throw new CuentaNoVerificadaException();
        }

        if (usuario.getIntentosFallidos() > 0 || usuario.getBloqueadoHasta() != null) {
            usuario.setIntentosFallidos(0);
            usuario.setBloqueadoHasta(null);
            usuarioRepositoryPort.guardar(usuario);
        }

        eventPublisher.publishEvent(new AccesoEvent(usuario.getId(), email, ipOrigen, ResultadoAuditoria.EXITO));

        String accessToken = tokenProviderPort.generarAccessToken(usuario);
        TokenGenerado refreshGenerado = tokenProviderPort.generarRefreshToken(usuario);
        persistirSesionRefresh(usuario.getId(), refreshGenerado, ipOrigen);

        return new LoginResultado(accessToken, refreshGenerado.token(), usuario);
    }

    private void registrarIntentoFallido(Usuario usuario) {
        int intentos = usuario.getIntentosFallidos() + 1;
        usuario.setIntentosFallidos(intentos);
        if (intentos >= MAX_INTENTOS_FALLIDOS) {
            usuario.setBloqueadoHasta(Instant.now().plus(BLOQUEO_MINUTOS, ChronoUnit.MINUTES));
        }
        usuarioRepositoryPort.guardar(usuario);
    }

    private void persistirSesionRefresh(Long usuarioId, TokenGenerado refreshGenerado, String ipOrigen) {
        sesionRefreshTokenRepositoryPort.guardar(SesionRefreshToken.builder()
                .usuarioId(usuarioId)
                .tokenHash(generadorTokenPort.hash(refreshGenerado.jti()))
                .expiraEn(refreshGenerado.expiracion())
                .ipOrigen(ipOrigen)
                .creadoEn(Instant.now())
                .build());
    }
}

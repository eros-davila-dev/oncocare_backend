package com.threepartners.oncologia.application.auth;

import com.threepartners.oncologia.domain.shared.exception.CredencialesInvalidasException;
import com.threepartners.oncologia.domain.usuario.GeneradorTokenPort;
import com.threepartners.oncologia.domain.usuario.SesionRefreshToken;
import com.threepartners.oncologia.domain.usuario.SesionRefreshTokenRepositoryPort;
import com.threepartners.oncologia.domain.usuario.TokenClaims;
import com.threepartners.oncologia.domain.usuario.TokenGenerado;
import com.threepartners.oncologia.domain.usuario.TokenProviderPort;
import com.threepartners.oncologia.domain.usuario.Usuario;
import com.threepartners.oncologia.domain.usuario.UsuarioRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Valida el refresh token (JWT) y ademas su sesion persistida (seccion 12):
 * cada uso rota el refresh token (revoca el anterior y emite uno nuevo), asi
 * que uno filtrado y reutilizado tras una rotacion legitima queda invalidado.
 */
@Service
@RequiredArgsConstructor
public class RefrescarTokenUseCase {

    private final TokenProviderPort tokenProviderPort;
    private final UsuarioRepositoryPort usuarioRepositoryPort;
    private final SesionRefreshTokenRepositoryPort sesionRefreshTokenRepositoryPort;
    private final GeneradorTokenPort generadorTokenPort;

    @Transactional
    public LoginResultado ejecutar(String refreshToken, String ipOrigen) {
        TokenClaims claims = tokenProviderPort.validar(refreshToken)
                .filter(TokenClaims::esRefresh)
                .orElseThrow(CredencialesInvalidasException::new);

        SesionRefreshToken sesion = sesionRefreshTokenRepositoryPort.buscarPorHash(generadorTokenPort.hash(claims.jti()))
                .filter(SesionRefreshToken::estaVigente)
                .orElseThrow(CredencialesInvalidasException::new);

        Usuario usuario = usuarioRepositoryPort.buscarPorId(claims.usuarioId())
                .filter(Usuario::isActivo)
                .orElseThrow(CredencialesInvalidasException::new);

        sesionRefreshTokenRepositoryPort.revocar(sesion.getId());

        String nuevoAccessToken = tokenProviderPort.generarAccessToken(usuario);
        TokenGenerado nuevoRefresh = tokenProviderPort.generarRefreshToken(usuario);
        sesionRefreshTokenRepositoryPort.guardar(SesionRefreshToken.builder()
                .usuarioId(usuario.getId())
                .tokenHash(generadorTokenPort.hash(nuevoRefresh.jti()))
                .expiraEn(nuevoRefresh.expiracion())
                .ipOrigen(ipOrigen)
                .creadoEn(Instant.now())
                .build());

        return new LoginResultado(nuevoAccessToken, nuevoRefresh.token(), usuario);
    }
}

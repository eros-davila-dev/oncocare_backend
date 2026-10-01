package com.threepartners.oncologia.application.auth;

import com.threepartners.oncologia.domain.auditoria.ResultadoAuditoria;
import com.threepartners.oncologia.domain.auditoria.event.CierreSesionEvent;
import com.threepartners.oncologia.domain.usuario.GeneradorTokenPort;
import com.threepartners.oncologia.domain.usuario.SesionRefreshTokenRepositoryPort;
import com.threepartners.oncologia.domain.usuario.TokenClaims;
import com.threepartners.oncologia.domain.usuario.TokenProviderPort;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Revoca la sesion de refresh asociada al token entregado. Es deliberadamente
 * tolerante (nunca lanza si el token ya es invalido o no tiene sesion
 * asociada): un logout siempre debe "tener exito" desde la perspectiva del
 * cliente.
 */
@Service
@RequiredArgsConstructor
public class CerrarSesionUseCase {

    private final TokenProviderPort tokenProviderPort;
    private final SesionRefreshTokenRepositoryPort sesionRefreshTokenRepositoryPort;
    private final GeneradorTokenPort generadorTokenPort;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void ejecutar(String refreshToken, String ipOrigen) {
        tokenProviderPort.validar(refreshToken)
                .filter(TokenClaims::esRefresh)
                .ifPresent(claims -> {
                    sesionRefreshTokenRepositoryPort.buscarPorHash(generadorTokenPort.hash(claims.jti()))
                            .ifPresent(sesion -> sesionRefreshTokenRepositoryPort.revocar(sesion.getId()));
                    eventPublisher.publishEvent(new CierreSesionEvent(
                            claims.usuarioId(), String.valueOf(claims.usuarioId()), ipOrigen, ResultadoAuditoria.EXITO));
                });
    }
}

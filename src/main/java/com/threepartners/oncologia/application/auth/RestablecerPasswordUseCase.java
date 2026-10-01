package com.threepartners.oncologia.application.auth;

import com.threepartners.oncologia.domain.auditoria.ResultadoAuditoria;
import com.threepartners.oncologia.domain.auditoria.event.PasswordRestablecidoEvent;
import com.threepartners.oncologia.domain.shared.exception.TokenInvalidoException;
import com.threepartners.oncologia.domain.usuario.GeneradorTokenPort;
import com.threepartners.oncologia.domain.usuario.PasswordEncoderPort;
import com.threepartners.oncologia.domain.usuario.SesionRefreshTokenRepositoryPort;
import com.threepartners.oncologia.domain.usuario.TipoTokenCuenta;
import com.threepartners.oncologia.domain.usuario.TokenAccionCuenta;
import com.threepartners.oncologia.domain.usuario.TokenAccionCuentaRepositoryPort;
import com.threepartners.oncologia.domain.usuario.Usuario;
import com.threepartners.oncologia.domain.usuario.UsuarioRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RestablecerPasswordUseCase {

    private final TokenAccionCuentaRepositoryPort tokenAccionCuentaRepositoryPort;
    private final UsuarioRepositoryPort usuarioRepositoryPort;
    private final SesionRefreshTokenRepositoryPort sesionRefreshTokenRepositoryPort;
    private final PasswordEncoderPort passwordEncoderPort;
    private final GeneradorTokenPort generadorTokenPort;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void ejecutar(String tokenPlano, String nuevaPasswordEnTextoPlano, String ipOrigen) {
        TokenAccionCuenta token = tokenAccionCuentaRepositoryPort.buscarPorHash(generadorTokenPort.hash(tokenPlano))
                .filter(t -> t.getTipo() == TipoTokenCuenta.RESET_PASSWORD)
                .filter(TokenAccionCuenta::estaVigente)
                .orElseThrow(TokenInvalidoException::new);

        Usuario cuenta = usuarioRepositoryPort.buscarPorId(token.getUsuarioId())
                .orElseThrow(TokenInvalidoException::new);

        cuenta.setPasswordHash(passwordEncoderPort.encriptar(nuevaPasswordEnTextoPlano));
        cuenta.setIntentosFallidos(0);
        cuenta.setBloqueadoHasta(null);
        usuarioRepositoryPort.guardar(cuenta);

        tokenAccionCuentaRepositoryPort.marcarUsado(token.getId());
        sesionRefreshTokenRepositoryPort.revocarTodasDeUsuario(cuenta.getId());

        eventPublisher.publishEvent(new PasswordRestablecidoEvent(
                cuenta.getId(), String.valueOf(cuenta.getId()), ipOrigen, ResultadoAuditoria.EXITO));
    }
}

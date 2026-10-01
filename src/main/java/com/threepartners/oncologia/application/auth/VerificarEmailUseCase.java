package com.threepartners.oncologia.application.auth;

import com.threepartners.oncologia.domain.auditoria.ResultadoAuditoria;
import com.threepartners.oncologia.domain.auditoria.event.CuentaVerificadaEvent;
import com.threepartners.oncologia.domain.shared.exception.TokenInvalidoException;
import com.threepartners.oncologia.domain.usuario.EstadoCuenta;
import com.threepartners.oncologia.domain.usuario.GeneradorTokenPort;
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
public class VerificarEmailUseCase {

    private final TokenAccionCuentaRepositoryPort tokenAccionCuentaRepositoryPort;
    private final UsuarioRepositoryPort usuarioRepositoryPort;
    private final GeneradorTokenPort generadorTokenPort;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void ejecutar(String tokenPlano, String ipOrigen) {
        TokenAccionCuenta token = tokenAccionCuentaRepositoryPort.buscarPorHash(generadorTokenPort.hash(tokenPlano))
                .filter(t -> t.getTipo() == TipoTokenCuenta.VERIFICACION_EMAIL)
                .filter(TokenAccionCuenta::estaVigente)
                .orElseThrow(TokenInvalidoException::new);

        Usuario cuenta = usuarioRepositoryPort.buscarPorId(token.getUsuarioId())
                .orElseThrow(TokenInvalidoException::new);

        cuenta.setEstadoCuenta(EstadoCuenta.ACTIVA);
        usuarioRepositoryPort.guardar(cuenta);
        tokenAccionCuentaRepositoryPort.marcarUsado(token.getId());

        eventPublisher.publishEvent(new CuentaVerificadaEvent(
                cuenta.getId(), String.valueOf(cuenta.getId()), ipOrigen, ResultadoAuditoria.EXITO));
    }
}

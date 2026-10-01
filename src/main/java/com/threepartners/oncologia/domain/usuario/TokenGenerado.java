package com.threepartners.oncologia.domain.usuario;

import java.time.Instant;

/**
 * Resultado de emitir un refresh token: el JWT compacto para entregar al
 * cliente, y el identificador unico (jti) mas su expiracion, que el llamador
 * necesita para persistir la sesion revocable sin tener que volver a
 * parsear el propio token.
 */
public record TokenGenerado(String token, String jti, Instant expiracion) {
}

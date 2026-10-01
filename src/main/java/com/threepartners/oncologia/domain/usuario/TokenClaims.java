package com.threepartners.oncologia.domain.usuario;

import java.time.Instant;

public record TokenClaims(Long usuarioId, String email, Rol rol, boolean esRefresh, Instant expiracion, String jti) {
}

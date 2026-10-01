package com.threepartners.oncologia.infrastructure.in.rest.dto.auth;

public record LoginResponseDto(String accessToken, String refreshToken, UsuarioResumenDto usuario) {
}

package com.threepartners.oncologia.application.auth;

import com.threepartners.oncologia.domain.usuario.Usuario;

public record LoginResultado(String accessToken, String refreshToken, Usuario usuario) {
}

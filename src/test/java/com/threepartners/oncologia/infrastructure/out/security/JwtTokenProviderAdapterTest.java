package com.threepartners.oncologia.infrastructure.out.security;

import com.threepartners.oncologia.domain.usuario.Rol;
import com.threepartners.oncologia.domain.usuario.TokenClaims;
import com.threepartners.oncologia.domain.usuario.TokenGenerado;
import com.threepartners.oncologia.domain.usuario.Usuario;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderAdapterTest {

    private final JwtProperties properties = new JwtProperties("secreto-de-pruebas-con-al-menos-32-caracteres", 15, 7);
    private final JwtTokenProviderAdapter provider = new JwtTokenProviderAdapter(properties);

    private Usuario usuarioDePrueba() {
        return Usuario.builder().id(1L).email("medico@threepartners.org").rol(Rol.MEDICO).build();
    }

    @Test
    void generaYValidaUnAccessTokenCorrectamente() {
        String token = provider.generarAccessToken(usuarioDePrueba());

        TokenClaims claims = provider.validar(token).orElseThrow();

        assertThat(claims.usuarioId()).isEqualTo(1L);
        assertThat(claims.rol()).isEqualTo(Rol.MEDICO);
        assertThat(claims.esRefresh()).isFalse();
    }

    @Test
    void marcaElRefreshTokenComoTal() {
        TokenGenerado generado = provider.generarRefreshToken(usuarioDePrueba());

        TokenClaims claims = provider.validar(generado.token()).orElseThrow();

        assertThat(claims.esRefresh()).isTrue();
        assertThat(claims.jti()).isEqualTo(generado.jti());
    }

    @Test
    void devuelveVacioParaUnTokenInvalido() {
        assertThat(provider.validar("token-invalido")).isEmpty();
    }
}

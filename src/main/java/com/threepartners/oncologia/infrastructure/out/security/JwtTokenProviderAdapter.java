package com.threepartners.oncologia.infrastructure.out.security;

import com.threepartners.oncologia.domain.usuario.Rol;
import com.threepartners.oncologia.domain.usuario.TokenClaims;
import com.threepartners.oncologia.domain.usuario.TokenGenerado;
import com.threepartners.oncologia.domain.usuario.TokenProviderPort;
import com.threepartners.oncologia.domain.usuario.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtTokenProviderAdapter implements TokenProviderPort {

    private static final String CLAIM_ROL = "rol";
    private static final String CLAIM_TIPO = "tipo";
    private static final String TIPO_ACCESS = "access";
    private static final String TIPO_REFRESH = "refresh";

    private final JwtProperties jwtProperties;

    private SecretKey clave() {
        return Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String generarAccessToken(Usuario usuario) {
        return generarToken(usuario, TIPO_ACCESS, jwtProperties.accessTokenMinutos(), ChronoUnit.MINUTES).token();
    }

    @Override
    public TokenGenerado generarRefreshToken(Usuario usuario) {
        return generarToken(usuario, TIPO_REFRESH, jwtProperties.refreshTokenDias(), ChronoUnit.DAYS);
    }

    private TokenGenerado generarToken(Usuario usuario, String tipo, long cantidad, ChronoUnit unidad) {
        Instant ahora = Instant.now();
        Instant expiracion = ahora.plus(cantidad, unidad);
        String jti = UUID.randomUUID().toString();

        String compacto = Jwts.builder()
                .subject(String.valueOf(usuario.getId()))
                .id(jti)
                .claim("email", usuario.getEmail())
                .claim(CLAIM_ROL, usuario.getRol().name())
                .claim(CLAIM_TIPO, tipo)
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(expiracion))
                .signWith(clave())
                .compact();

        return new TokenGenerado(compacto, jti, expiracion);
    }

    @Override
    public Optional<TokenClaims> validar(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(clave())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            boolean esRefresh = TIPO_REFRESH.equals(claims.get(CLAIM_TIPO, String.class));

            return Optional.of(new TokenClaims(
                    Long.valueOf(claims.getSubject()),
                    claims.get("email", String.class),
                    Rol.valueOf(claims.get(CLAIM_ROL, String.class)),
                    esRefresh,
                    claims.getExpiration().toInstant(),
                    claims.getId()));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}

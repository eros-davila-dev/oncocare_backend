package com.threepartners.oncologia.config;

import com.threepartners.oncologia.domain.usuario.TokenClaims;
import com.threepartners.oncologia.domain.usuario.TokenProviderPort;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String PREFIJO_BEARER = "Bearer ";

    private final TokenProviderPort tokenProviderPort;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {

        String cabeceraAuth = request.getHeader("Authorization");

        if (cabeceraAuth != null && cabeceraAuth.startsWith(PREFIJO_BEARER)) {
            String token = cabeceraAuth.substring(PREFIJO_BEARER.length());
            Optional<TokenClaims> claims = tokenProviderPort.validar(token).filter(c -> !c.esRefresh());

            if (claims.isPresent() && SecurityContextHolder.getContext().getAuthentication() == null) {
                TokenClaims c = claims.get();
                var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + c.rol().name()));
                var authentication = new UsernamePasswordAuthenticationToken(c.usuarioId(), null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }
}

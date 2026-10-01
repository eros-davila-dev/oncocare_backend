package com.threepartners.oncologia.infrastructure.in.rest;

import com.threepartners.oncologia.domain.usuario.Rol;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.stream.Stream;

/**
 * Extrae el id del usuario autenticado (colocado como principal por
 * JwtAuthFilter) y la IP de origen de la peticion, datos requeridos por casi
 * todo caso de uso para completar el evento de auditoria correspondiente.
 */
public final class AutenticacionActual {

    private AutenticacionActual() {
    }

    public static Long usuarioId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            return null;
        }
        if (authentication.getPrincipal() instanceof Long id) {
            return id;
        }
        return null;
    }

    /**
     * Rol tal como lo coloco JwtAuthFilter ("ROLE_&lt;rol&gt;"), necesario en
     * los casos de uso que aplican reglas distintas segun si quien opera es
     * staff o un paciente autoservido (seccion 31).
     */
    public static Rol rol() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(authority -> authority.replaceFirst("^ROLE_", ""))
                .flatMap(AutenticacionActual::comoRol)
                .findFirst()
                .orElse(null);
    }

    /**
     * En rutas publicas (ej. el chatbot) Spring Security coloca de todas
     * formas un AnonymousAuthenticationToken con la authority ROLE_ANONYMOUS,
     * que no es un Rol del dominio: se descarta en vez de reventar con
     * IllegalArgumentException.
     */
    private static Stream<Rol> comoRol(String nombre) {
        try {
            return Stream.of(Rol.valueOf(nombre));
        } catch (IllegalArgumentException e) {
            return Stream.empty();
        }
    }

    public static String ipOrigen(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

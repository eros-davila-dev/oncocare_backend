package com.threepartners.oncologia.domain.usuario;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Registro persistente de cada refresh token emitido (solo su hash), para
 * poder revocarlo en logout, en rotacion (cada /auth/refresh revoca el
 * anterior y emite uno nuevo) o al restablecer la contrasena. El access
 * token sigue siendo puramente stateless (vida corta, no se persiste).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SesionRefreshToken {

    private Long id;
    private Long usuarioId;
    private String tokenHash;
    private Instant expiraEn;
    private Instant revocadoEn;
    private String ipOrigen;
    private Instant creadoEn;

    public boolean estaVigente() {
        return revocadoEn == null && expiraEn != null && expiraEn.isAfter(Instant.now());
    }
}

package com.threepartners.oncologia.domain.usuario;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Token de un solo uso para verificacion de correo o restablecimiento de
 * contrasena (seccion 9). Solo se persiste el hash; el valor en texto plano
 * viaja unicamente en el enlace enviado por correo y nunca se guarda.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenAccionCuenta {

    private Long id;
    private Long usuarioId;
    private TipoTokenCuenta tipo;
    private String tokenHash;
    private Instant expiraEn;
    private Instant usadoEn;
    private Instant creadoEn;

    public boolean estaVigente() {
        return usadoEn == null && expiraEn != null && expiraEn.isAfter(Instant.now());
    }
}

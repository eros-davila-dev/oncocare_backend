package com.threepartners.oncologia.domain.paciente;

import com.threepartners.oncologia.domain.shared.exception.ValidacionDeNegocioException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Token de un solo uso que viaja en el enlace t.me/<bot>?start=<token>. Solo
 * se guarda su hash: quien lea la base no puede vincular chats ajenos.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenVinculacionTelegram {

    private Long id;
    private Long pacienteId;
    private String tokenHash;
    private Instant expiraEn;
    private Instant usadoEn;

    public void usar(Instant ahora) {
        if (usadoEn != null) {
            throw new ValidacionDeNegocioException("El enlace de vinculacion ya fue usado");
        }
        if (ahora.isAfter(expiraEn)) {
            throw new ValidacionDeNegocioException("El enlace de vinculacion vencio: genere uno nuevo");
        }
        this.usadoEn = ahora;
    }
}

package com.threepartners.oncologia.domain.paciente;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Paso intermedio de la vinculacion por telefono: el numero compartido en el
 * bot encontro pacientes (como titular o como referido) y falta que el
 * usuario confirme con los 3 ultimos digitos del DNI del paciente.
 *
 * Por que se pide: si la operadora reasigno el numero de un paciente a otra
 * persona, el numero solo no basta para saber que es el. Tambien distingue a
 * dos pacientes que comparten un celular.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VinculacionTelegramPendiente {

    public static final int MAXIMO_INTENTOS = 3;
    public static final Duration VIGENCIA = Duration.ofMinutes(10);
    public static final Duration BLOQUEO = Duration.ofMinutes(30);

    private Long chatId;
    @Builder.Default
    private List<Long> titulares = new ArrayList<>();
    @Builder.Default
    private List<Long> referidos = new ArrayList<>();
    private int intentos;
    private Instant expiraEn;
    private Instant bloqueadoHasta;

    public static VinculacionTelegramPendiente nueva(Long chatId, List<Long> titulares, List<Long> referidos, Instant ahora) {
        return VinculacionTelegramPendiente.builder()
                .chatId(chatId)
                .titulares(new ArrayList<>(titulares))
                .referidos(new ArrayList<>(referidos))
                .expiraEn(ahora.plus(VIGENCIA))
                .build();
    }

    public boolean bloqueado(Instant ahora) {
        return bloqueadoHasta != null && ahora.isBefore(bloqueadoHasta);
    }

    /** Hay candidatos esperando los digitos y no vencio el plazo. */
    public boolean esperandoDigitos(Instant ahora) {
        return !bloqueado(ahora) && ahora.isBefore(expiraEn) && !(titulares.isEmpty() && referidos.isEmpty());
    }

    /** Un intento fallido; al tercero se bloquea el chat y se descartan los candidatos. */
    public void fallo(Instant ahora) {
        intentos++;
        if (intentos >= MAXIMO_INTENTOS) {
            bloqueadoHasta = ahora.plus(BLOQUEO);
            titulares.clear();
            referidos.clear();
        }
    }

    public int intentosRestantes() {
        return Math.max(0, MAXIMO_INTENTOS - intentos);
    }
}

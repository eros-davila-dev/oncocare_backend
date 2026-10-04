package com.threepartners.oncologia.domain.paciente;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TelefonoTest {

    @Test
    void elMismoNumeroEscritoDeFormasDistintasEsIgual() {
        assertThat(Telefono.normalizar("+51 904 049 494")).isEqualTo("904049494");
        assertThat(Telefono.mismos("51904049494", "904-049-494")).isTrue();
        assertThat(Telefono.mismos("(01) 904 049 494", "904049494")).isTrue();
        assertThat(Telefono.mismos("904049494", "958033925")).isFalse();
    }

    @Test
    void unNumeroIncompletoNoCoincideConNada() {
        assertThat(Telefono.normalizar("12345")).isEmpty();
        assertThat(Telefono.normalizar(null)).isEmpty();
        assertThat(Telefono.mismos("12345", "12345")).isFalse();
    }
}

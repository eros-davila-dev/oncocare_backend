package com.threepartners.oncologia.infrastructure.out.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CifradorSecretosTest {

    private static final JwtProperties JWT = new JwtProperties("secreto-jwt-de-prueba-con-32-caracteres", 15, 7);

    @Test
    void cifraYDescifraSinGuardarElTextoPlano() {
        CifradorSecretos cifrador = new CifradorSecretos(new CifradoProperties("clave-de-cifrado"), JWT);

        String cifrado = cifrador.cifrar("AIzaSyClaveDeGemini");

        assertThat(cifrado).startsWith("v1:").doesNotContain("AIzaSyClaveDeGemini");
        assertThat(cifrador.descifrar(cifrado)).isEqualTo("AIzaSyClaveDeGemini");
    }

    @Test
    void cadaCifradoUsaUnIvDistinto() {
        CifradorSecretos cifrador = new CifradorSecretos(new CifradoProperties("clave-de-cifrado"), JWT);

        assertThat(cifrador.cifrar("misma")).isNotEqualTo(cifrador.cifrar("misma"));
    }

    @Test
    void sinClavePropiaUsaJwtSecret() {
        CifradorSecretos conJwt = new CifradorSecretos(new CifradoProperties(""), JWT);
        CifradorSecretos otraInstancia = new CifradorSecretos(new CifradoProperties(null), JWT);

        assertThat(otraInstancia.descifrar(conJwt.cifrar("valor"))).isEqualTo("valor");
    }

    @Test
    void conOtraClaveNoSePuedeDescifrar() {
        String cifrado = new CifradorSecretos(new CifradoProperties("clave-a"), JWT).cifrar("valor");
        CifradorSecretos otra = new CifradorSecretos(new CifradoProperties("clave-b"), JWT);

        assertThatThrownBy(() -> otra.descifrar(cifrado)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rechazaTextoAlteradoOConFormatoDesconocido() {
        CifradorSecretos cifrador = new CifradorSecretos(new CifradoProperties("clave"), JWT);
        String cifrado = cifrador.cifrar("valor");
        String alterado = cifrado.substring(0, cifrado.length() - 2) + (cifrado.endsWith("A") ? "BB" : "AA");

        assertThatThrownBy(() -> cifrador.descifrar(alterado)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> cifrador.descifrar("texto-plano")).isInstanceOf(IllegalArgumentException.class);
    }
}

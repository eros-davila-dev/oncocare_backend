package com.threepartners.oncologia.infrastructure.out.security;

import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Cifrado simetrico AES-256-GCM para secretos guardados en la base: un
 * volcado o un respaldo de PostgreSQL no expone la clave de Gemini. GCM
 * ademas detecta si el texto cifrado fue alterado.
 *
 * Formato: "v1:" + Base64(iv de 12 bytes || texto cifrado con etiqueta).
 */
@Component
public class CifradorSecretos {

    private static final String PREFIJO = "v1:";
    private static final int BYTES_IV = 12;
    private static final int BITS_ETIQUETA = 128;

    private final SecretKeySpec clave;
    private final SecureRandom aleatorio = new SecureRandom();

    public CifradorSecretos(CifradoProperties cifrado, JwtProperties jwt) {
        String origen = cifrado != null && cifrado.clave() != null && !cifrado.clave().isBlank()
                ? cifrado.clave() : jwt.secret();
        this.clave = new SecretKeySpec(sha256(origen), "AES");
    }

    public String cifrar(String textoPlano) {
        try {
            byte[] iv = new byte[BYTES_IV];
            aleatorio.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, clave, new GCMParameterSpec(BITS_ETIQUETA, iv));
            byte[] cifrado = cipher.doFinal(textoPlano.getBytes(StandardCharsets.UTF_8));
            byte[] todo = new byte[iv.length + cifrado.length];
            System.arraycopy(iv, 0, todo, 0, iv.length);
            System.arraycopy(cifrado, 0, todo, iv.length, cifrado.length);
            return PREFIJO + Base64.getEncoder().encodeToString(todo);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo cifrar el secreto", e);
        }
    }

    /** @throws IllegalArgumentException si no se puede descifrar (otra clave o dato alterado) */
    public String descifrar(String textoCifrado) {
        if (textoCifrado == null || !textoCifrado.startsWith(PREFIJO)) {
            throw new IllegalArgumentException("Formato de secreto cifrado desconocido");
        }
        try {
            byte[] todo = Base64.getDecoder().decode(textoCifrado.substring(PREFIJO.length()));
            if (todo.length <= BYTES_IV) {
                throw new IllegalArgumentException("Secreto cifrado incompleto");
            }
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, clave, new GCMParameterSpec(BITS_ETIQUETA, todo, 0, BYTES_IV));
            byte[] plano = cipher.doFinal(todo, BYTES_IV, todo.length - BYTES_IV);
            return new String(plano, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException("No se pudo descifrar el secreto (cambio la clave de cifrado?)", e);
        }
    }

    private static byte[] sha256(String texto) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}

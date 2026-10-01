package com.threepartners.oncologia.infrastructure.in.rest;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Cada dispositivo externo se registra con su propia credencial (seccion 14):
 * nunca comparte el JWT de un usuario humano. Solo se persiste el hash,
 * nunca la credencial en texto plano.
 */
@Component
public class CredencialDispositivoHasher {

    public String hash(String credencialEnTextoPlano) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(credencialEnTextoPlano.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo de hash no disponible", e);
        }
    }
}

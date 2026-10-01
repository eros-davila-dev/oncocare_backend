package com.threepartners.oncologia.infrastructure.out.security;

import com.threepartners.oncologia.domain.usuario.GeneradorTokenPort;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

@Component
public class GeneradorTokenSeguroAdapter implements GeneradorTokenPort {

    private static final int LONGITUD_BYTES = 32;

    private final SecureRandom random = new SecureRandom();

    @Override
    public String generarToken() {
        byte[] bytes = new byte[LONGITUD_BYTES];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    @Override
    public String hash(String valorEnTextoPlano) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(valorEnTextoPlano.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}

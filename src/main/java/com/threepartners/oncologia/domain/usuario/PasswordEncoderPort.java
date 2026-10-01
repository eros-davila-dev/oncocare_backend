package com.threepartners.oncologia.domain.usuario;

public interface PasswordEncoderPort {

    String encriptar(String passwordEnTextoPlano);

    boolean coincide(String passwordEnTextoPlano, String passwordHash);
}

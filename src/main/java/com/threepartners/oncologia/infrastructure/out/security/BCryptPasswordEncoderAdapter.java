package com.threepartners.oncologia.infrastructure.out.security;

import com.threepartners.oncologia.domain.usuario.PasswordEncoderPort;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BCryptPasswordEncoderAdapter implements PasswordEncoderPort {

    private final PasswordEncoder passwordEncoder;

    @Override
    public String encriptar(String passwordEnTextoPlano) {
        return passwordEncoder.encode(passwordEnTextoPlano);
    }

    @Override
    public boolean coincide(String passwordEnTextoPlano, String passwordHash) {
        return passwordEncoder.matches(passwordEnTextoPlano, passwordHash);
    }
}

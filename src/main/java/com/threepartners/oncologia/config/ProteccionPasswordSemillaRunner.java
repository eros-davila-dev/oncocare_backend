package com.threepartners.oncologia.config;

import com.threepartners.oncologia.application.auth.ProtegerPasswordSemillaUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Revisa la contrasena de la semilla en cada arranque (ver
 * ProtegerPasswordSemillaUseCase). Si lanza la excepcion, la aplicacion no
 * arranca: es preferible a quedar expuesta.
 */
@Component
@RequiredArgsConstructor
public class ProteccionPasswordSemillaRunner implements ApplicationRunner {

    private final ProtegerPasswordSemillaUseCase protegerPasswordSemillaUseCase;

    @Value("${app.seguridad.admin-password-inicial:}")
    private String passwordInicial;

    @Value("${app.seguridad.exigir-cambio-password-semilla:false}")
    private boolean exigirCambio;

    @Override
    public void run(ApplicationArguments args) {
        protegerPasswordSemillaUseCase.ejecutar(passwordInicial, exigirCambio);
    }
}

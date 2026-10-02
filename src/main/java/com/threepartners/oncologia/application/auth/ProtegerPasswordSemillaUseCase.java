package com.threepartners.oncologia.application.auth;

import com.threepartners.oncologia.domain.auditoria.event.OperacionAuditadaEvent;
import com.threepartners.oncologia.domain.usuario.PasswordEncoderPort;
import com.threepartners.oncologia.domain.usuario.SesionRefreshTokenRepositoryPort;
import com.threepartners.oncologia.domain.usuario.Usuario;
import com.threepartners.oncologia.domain.usuario.UsuarioRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * La migracion V2 crea al administrador con una contrasena que esta publicada
 * en el README. En un servidor real eso es una puerta abierta a datos de
 * salud, asi que al arrancar:
 *
 * - si se definio una contrasena inicial (ADMIN_PASSWORD_INICIAL), reemplaza
 *   la de la semilla, cierra las sesiones abiertas y lo audita;
 * - si no, y el entorno lo exige (perfil prod), impide el arranque;
 * - en desarrollo solo advierte.
 *
 * Una vez cambiada, la variable se ignora: nunca pisa una contrasena elegida.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProtegerPasswordSemillaUseCase {

    static final String EMAIL_ADMIN_SEMILLA = "admin@threepartners.org";
    static final String PASSWORD_SEMILLA = "Admin123!";
    static final int LONGITUD_MINIMA = 12;

    private final UsuarioRepositoryPort usuarioRepositoryPort;
    private final PasswordEncoderPort passwordEncoderPort;
    private final SesionRefreshTokenRepositoryPort sesionRefreshTokenRepositoryPort;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Resultado ejecutar(String passwordInicial, boolean exigirCambio) {
        Usuario admin = usuarioRepositoryPort.buscarPorEmail(EMAIL_ADMIN_SEMILLA).orElse(null);
        if (admin == null || !passwordEncoderPort.coincide(PASSWORD_SEMILLA, admin.getPasswordHash())) {
            return Resultado.SIN_PASSWORD_SEMILLA;
        }

        if (passwordInicial != null && !passwordInicial.isBlank()) {
            validar(passwordInicial);
            admin.setPasswordHash(passwordEncoderPort.encriptar(passwordInicial));
            admin.setIntentosFallidos(0);
            admin.setBloqueadoHasta(null);
            usuarioRepositoryPort.guardar(admin);
            sesionRefreshTokenRepositoryPort.revocarTodasDeUsuario(admin.getId());
            eventPublisher.publishEvent(OperacionAuditadaEvent.exito(null, "PASSWORD_SEMILLA_REEMPLAZADA",
                    "USUARIO", admin.getId(), null, null, null));
            log.info("Se reemplazo la contrasena de la semilla del administrador");
            return Resultado.REEMPLAZADA;
        }

        if (exigirCambio) {
            throw new IllegalStateException("El administrador " + EMAIL_ADMIN_SEMILLA + " conserva la contrasena "
                    + "publicada en el README. Defina ADMIN_PASSWORD_INICIAL (minimo " + LONGITUD_MINIMA
                    + " caracteres) para reemplazarla en este arranque.");
        }
        log.warn("El administrador {} conserva la contrasena de la semilla: cambiela antes de usar datos reales",
                EMAIL_ADMIN_SEMILLA);
        return Resultado.SEMILLA_ACTIVA;
    }

    private static void validar(String password) {
        if (password.length() < LONGITUD_MINIMA || password.equals(PASSWORD_SEMILLA)) {
            throw new IllegalStateException("ADMIN_PASSWORD_INICIAL debe tener al menos " + LONGITUD_MINIMA
                    + " caracteres y ser distinta de la contrasena de la semilla");
        }
    }

    public enum Resultado {
        SIN_PASSWORD_SEMILLA,
        REEMPLAZADA,
        SEMILLA_ACTIVA
    }
}

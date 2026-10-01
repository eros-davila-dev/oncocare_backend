package com.threepartners.oncologia.application.auth;

import com.threepartners.oncologia.config.FrontendProperties;
import com.threepartners.oncologia.config.RegistroProperties;
import com.threepartners.oncologia.domain.auditoria.ResultadoAuditoria;
import com.threepartners.oncologia.domain.auditoria.event.CuentaRegistradaEvent;
import com.threepartners.oncologia.domain.shared.exception.RegistroNoDisponibleException;
import com.threepartners.oncologia.domain.usuario.EstadoCuenta;
import com.threepartners.oncologia.domain.usuario.GeneradorTokenPort;
import com.threepartners.oncologia.domain.usuario.PasswordEncoderPort;
import com.threepartners.oncologia.domain.usuario.Rol;
import com.threepartners.oncologia.domain.usuario.TipoTokenCuenta;
import com.threepartners.oncologia.domain.usuario.TokenAccionCuenta;
import com.threepartners.oncologia.domain.usuario.TokenAccionCuentaRepositoryPort;
import com.threepartners.oncologia.domain.usuario.Usuario;
import com.threepartners.oncologia.domain.usuario.UsuarioRepositoryPort;
import com.threepartners.oncologia.infrastructure.out.notification.N8nNotificationAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * Paso 1 del flujo de autoservicio (seccion 7): crea la CUENTA en estado
 * PENDIENTE_VERIFICACION y dispara el envio del correo de verificacion via
 * n8n. La creacion del registro clinico (Paciente) ocurre despues, en
 * CompletarPerfilPacienteUseCase, una vez verificado el correo.
 */
@Service
@RequiredArgsConstructor
public class RegistrarCuentaPacienteUseCase {

    private static final Duration VIGENCIA_TOKEN_VERIFICACION = Duration.ofHours(24);

    private final UsuarioRepositoryPort usuarioRepositoryPort;
    private final PasswordEncoderPort passwordEncoderPort;
    private final TokenAccionCuentaRepositoryPort tokenAccionCuentaRepositoryPort;
    private final GeneradorTokenPort generadorTokenPort;
    private final N8nNotificationAdapter n8nNotificationAdapter;
    private final FrontendProperties frontendProperties;
    private final RegistroProperties registroProperties;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void ejecutar(String nombres, String email, String passwordEnTextoPlano, String ipOrigen) {
        if (!registroProperties.pacientesHabilitado()) {
            throw new RegistroNoDisponibleException();
        }

        String emailNormalizado = email.trim().toLowerCase();

        if (usuarioRepositoryPort.existePorEmail(emailNormalizado)) {
            // Seccion 9: no se revela si el correo ya esta registrado; el
            // llamador (controller) siempre responde con el mismo mensaje.
            return;
        }

        Usuario cuenta = Usuario.builder()
                .nombres(nombres.trim())
                .email(emailNormalizado)
                .passwordHash(passwordEncoderPort.encriptar(passwordEnTextoPlano))
                .rol(Rol.PACIENTE)
                .estadoCuenta(EstadoCuenta.PENDIENTE_VERIFICACION)
                .intentosFallidos(0)
                .activo(true)
                .fechaCreacion(Instant.now())
                .build();

        Usuario guardada = usuarioRepositoryPort.guardar(cuenta);

        enviarTokenVerificacion(guardada);

        eventPublisher.publishEvent(new CuentaRegistradaEvent(
                guardada.getId(), String.valueOf(guardada.getId()), ipOrigen, ResultadoAuditoria.EXITO));
    }

    private void enviarTokenVerificacion(Usuario cuenta) {
        String tokenPlano = generadorTokenPort.generarToken();
        tokenAccionCuentaRepositoryPort.guardar(TokenAccionCuenta.builder()
                .usuarioId(cuenta.getId())
                .tipo(TipoTokenCuenta.VERIFICACION_EMAIL)
                .tokenHash(generadorTokenPort.hash(tokenPlano))
                .expiraEn(Instant.now().plus(VIGENCIA_TOKEN_VERIFICACION))
                .creadoEn(Instant.now())
                .build());

        n8nNotificationAdapter.dispararWorkflow("/webhook/notificaciones/verificacion-email", Map.of(
                "email", cuenta.getEmail(),
                "nombre", cuenta.getNombres(),
                "urlVerificacion", frontendProperties.baseUrl() + "/auth/verificar-email?token=" + tokenPlano));
    }
}

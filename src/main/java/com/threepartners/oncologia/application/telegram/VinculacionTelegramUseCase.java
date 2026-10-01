package com.threepartners.oncologia.application.telegram;

import com.threepartners.oncologia.config.TelegramProperties;
import com.threepartners.oncologia.domain.auditoria.event.OperacionAuditadaEvent;
import com.threepartners.oncologia.domain.paciente.Paciente;
import com.threepartners.oncologia.domain.paciente.PacienteRepositoryPort;
import com.threepartners.oncologia.domain.paciente.TokenVinculacionTelegram;
import com.threepartners.oncologia.domain.paciente.TokenVinculacionTelegramRepositoryPort;
import com.threepartners.oncologia.domain.shared.exception.RecursoNoEncontradoException;
import com.threepartners.oncologia.domain.shared.exception.ValidacionDeNegocioException;
import com.threepartners.oncologia.domain.usuario.GeneradorTokenPort;
import com.threepartners.oncologia.domain.usuario.Rol;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Un bot de Telegram solo puede escribir a quien le inicio una conversacion.
 * Vincular el chat con el paciente es lo que permite enviarle recordatorios
 * (indicador TNS) y reconocerlo cuando escribe al bot (indicador NCA).
 *
 * Flujo: el portal o recepcion generan un enlace t.me/<bot>?start=<token>
 * (tambien como QR); el paciente lo abre, Telegram envia "/start <token>" al
 * bot, n8n lo reenvia aqui y el chat queda vinculado.
 */
@Service
@RequiredArgsConstructor
public class VinculacionTelegramUseCase {

    private final PacienteRepositoryPort pacienteRepositoryPort;
    private final TokenVinculacionTelegramRepositoryPort tokenRepositoryPort;
    private final GeneradorTokenPort generadorTokenPort;
    private final TelegramProperties telegramProperties;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    /**
     * El paciente genera su propio enlace (pacienteId se ignora); el personal
     * lo genera para el paciente que atiende en recepcion.
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA', 'PACIENTE')")
    @Transactional
    public EnlaceVinculacion generarEnlace(Long pacienteId, Long usuarioId, Rol rol, String ipOrigen) {
        if (!telegramProperties.configurado()) {
            throw new ValidacionDeNegocioException(
                    "Telegram aun no esta configurado (falta TELEGRAM_BOT_USERNAME en el servidor)");
        }
        Paciente paciente = resolverPaciente(pacienteId, usuarioId, rol);
        String tokenPlano = generadorTokenPort.generarToken();
        Instant expira = clock.instant().plus(Duration.ofMinutes(telegramProperties.minutosValidezEnlace()));
        tokenRepositoryPort.guardar(TokenVinculacionTelegram.builder()
                .pacienteId(paciente.getId())
                .tokenHash(generadorTokenPort.hash(tokenPlano))
                .expiraEn(expira)
                .build());
        eventPublisher.publishEvent(OperacionAuditadaEvent.exito(usuarioId, "TELEGRAM_ENLACE_GENERADO", "PACIENTE",
                paciente.getId(), null, "expira=" + expira, ipOrigen));
        return new EnlaceVinculacion("https://t.me/%s?start=%s".formatted(telegramProperties.botUsername(), tokenPlano),
                expira, paciente.tieneTelegram());
    }

    /** Invocado por n8n al recibir "/start <token>". Un chat solo puede estar vinculado a un paciente. */
    @Transactional
    public Paciente vincular(String tokenPlano, Long chatId) {
        TokenVinculacionTelegram token = tokenRepositoryPort.buscarPorHash(generadorTokenPort.hash(tokenPlano))
                .orElseThrow(() -> new ValidacionDeNegocioException("El enlace de vinculacion no es valido"));
        token.usar(clock.instant());
        tokenRepositoryPort.guardar(token);

        pacienteRepositoryPort.buscarPorTelegramChatId(chatId)
                .filter(otro -> !otro.getId().equals(token.getPacienteId()))
                .ifPresent(otro -> {
                    otro.desvincularTelegram();
                    pacienteRepositoryPort.guardar(otro);
                });

        Paciente paciente = pacienteRepositoryPort.buscarPorId(token.getPacienteId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Paciente", token.getPacienteId()));
        paciente.vincularTelegram(chatId, clock.instant());
        Paciente guardado = pacienteRepositoryPort.guardar(paciente);
        eventPublisher.publishEvent(OperacionAuditadaEvent.exito(null, "TELEGRAM_VINCULADO", "PACIENTE",
                guardado.getId(), null, "vinculado=true", "telegram"));
        return guardado;
    }

    /** "/stop" en el bot o "Desvincular" en el portal: el paciente deja de recibir mensajes. */
    @Transactional
    public boolean desvincularPorChat(Long chatId) {
        return pacienteRepositoryPort.buscarPorTelegramChatId(chatId).map(paciente -> {
            paciente.desvincularTelegram();
            pacienteRepositoryPort.guardar(paciente);
            eventPublisher.publishEvent(OperacionAuditadaEvent.exito(null, "TELEGRAM_DESVINCULADO", "PACIENTE",
                    paciente.getId(), "vinculado=true", "vinculado=false", "telegram"));
            return true;
        }).orElse(false);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA', 'PACIENTE')")
    @Transactional
    public void desvincular(Long pacienteId, Long usuarioId, Rol rol, String ipOrigen) {
        Paciente paciente = resolverPaciente(pacienteId, usuarioId, rol);
        paciente.desvincularTelegram();
        pacienteRepositoryPort.guardar(paciente);
        eventPublisher.publishEvent(OperacionAuditadaEvent.exito(usuarioId, "TELEGRAM_DESVINCULADO", "PACIENTE",
                paciente.getId(), "vinculado=true", "vinculado=false", ipOrigen));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA', 'MEDICO', 'PACIENTE')")
    @Transactional(readOnly = true)
    public EstadoVinculacion estado(Long pacienteId, Long usuarioId, Rol rol) {
        Paciente paciente = resolverPaciente(pacienteId, usuarioId, rol);
        return new EstadoVinculacion(paciente.tieneTelegram(), paciente.getTelegramVinculadoEn(), telegramProperties.configurado());
    }

    private Paciente resolverPaciente(Long pacienteId, Long usuarioId, Rol rol) {
        if (rol == Rol.PACIENTE) {
            return pacienteRepositoryPort.buscarPorUsuarioId(usuarioId)
                    .orElseThrow(() -> new ValidacionDeNegocioException("Complete su perfil de paciente primero"));
        }
        if (pacienteId == null) {
            throw new ValidacionDeNegocioException("Indique el paciente");
        }
        return pacienteRepositoryPort.buscarPorId(pacienteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Paciente", pacienteId));
    }

    public record EnlaceVinculacion(String enlace, Instant expiraEn, boolean yaVinculado) {
    }

    public record EstadoVinculacion(boolean vinculado, Instant vinculadoEn, boolean telegramDisponible) {
    }
}

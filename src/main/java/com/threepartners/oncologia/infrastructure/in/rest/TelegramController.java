package com.threepartners.oncologia.infrastructure.in.rest;

import com.threepartners.oncologia.application.telegram.VinculacionTelegramUseCase;
import com.threepartners.oncologia.config.TelegramProperties;
import com.threepartners.oncologia.application.telegram.VinculacionTelegramUseCase.EnlaceVinculacion;
import com.threepartners.oncologia.application.telegram.VinculacionTelegramUseCase.EstadoVinculacion;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Vinculacion del chat de Telegram del paciente (portal: su propio chat;
 * recepcion: el del paciente que atiende, con pacienteId).
 */
@RestController
@RequestMapping("/api/v1/telegram")
@RequiredArgsConstructor
public class TelegramController {

    private final VinculacionTelegramUseCase vinculacionTelegramUseCase;
    private final TelegramProperties telegramProperties;

    /**
     * Enlace generico del bot (el mismo para todos) para el QR comun y la guia
     * imprimible. Publico: no identifica a nadie; la vinculacion se completa
     * en el bot con "Compartir mi numero" y los digitos del DNI.
     */
    @GetMapping("/bot")
    public BotPublico bot() {
        return telegramProperties.configurado()
                ? new BotPublico(true, telegramProperties.botUsername(), "https://t.me/" + telegramProperties.botUsername())
                : new BotPublico(false, null, null);
    }

    public record BotPublico(boolean disponible, String usuario, String enlace) {
    }

    @GetMapping("/estado")
    public EstadoVinculacion estado(@RequestParam(required = false) Long pacienteId) {
        return vinculacionTelegramUseCase.estado(pacienteId, AutenticacionActual.usuarioId(), AutenticacionActual.rol());
    }

    @PostMapping("/enlace")
    public EnlaceVinculacion generarEnlace(@RequestParam(required = false) Long pacienteId, HttpServletRequest request) {
        return vinculacionTelegramUseCase.generarEnlace(pacienteId, AutenticacionActual.usuarioId(),
                AutenticacionActual.rol(), AutenticacionActual.ipOrigen(request));
    }

    @DeleteMapping("/vinculo-referido")
    public ResponseEntity<Void> desvincularReferido(@RequestParam(required = false) Long pacienteId, HttpServletRequest request) {
        vinculacionTelegramUseCase.desvincularReferido(pacienteId, AutenticacionActual.usuarioId(), AutenticacionActual.rol(),
                AutenticacionActual.ipOrigen(request));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/vinculo")
    public ResponseEntity<Void> desvincular(@RequestParam(required = false) Long pacienteId, HttpServletRequest request) {
        vinculacionTelegramUseCase.desvincular(pacienteId, AutenticacionActual.usuarioId(), AutenticacionActual.rol(),
                AutenticacionActual.ipOrigen(request));
        return ResponseEntity.noContent().build();
    }
}

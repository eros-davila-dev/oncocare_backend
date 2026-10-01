package com.threepartners.oncologia.infrastructure.in.rest;

import com.threepartners.oncologia.application.notificacion.RegistrarNotificacionUseCase;
import com.threepartners.oncologia.domain.notificacion.Notificacion;
import com.threepartners.oncologia.infrastructure.in.rest.dto.notificacion.NotificacionRequestDto;
import com.threepartners.oncologia.infrastructure.in.rest.dto.notificacion.NotificacionResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Consumido por n8n (seccion 15, flujo 2) para dejar constancia del resultado
 * del envio de un recordatorio de cita por WhatsApp o correo.
 */
@RestController
@RequestMapping("/api/v1/notificaciones")
@RequiredArgsConstructor
public class NotificacionController {

    private final RegistrarNotificacionUseCase registrarNotificacionUseCase;
    private final WebhookSecretValidator webhookSecretValidator;

    @PostMapping("/callback")
    public ResponseEntity<NotificacionResponseDto> registrarResultadoEnvio(
            @RequestHeader(value = "X-Webhook-Secret", required = false) String secreto,
            @Valid @RequestBody NotificacionRequestDto dto) {

        webhookSecretValidator.validar(secreto);

        var notificacion = registrarNotificacionUseCase.ejecutar(Notificacion.builder()
                .citaId(dto.citaId())
                .canal(dto.canal())
                .estadoEnvio(dto.estadoEnvio())
                .build());

        return ResponseEntity.status(HttpStatus.CREATED).body(new NotificacionResponseDto(
                notificacion.getId(), notificacion.getCitaId(), notificacion.getCanal(),
                notificacion.getEstadoEnvio(), notificacion.getFechaEnvio()));
    }
}

package com.threepartners.oncologia.infrastructure.in.rest.dto.notificacion;

import com.threepartners.oncologia.domain.notificacion.CanalNotificacion;
import com.threepartners.oncologia.domain.notificacion.EstadoEnvio;

import java.time.Instant;

public record NotificacionResponseDto(Long id, Long citaId, CanalNotificacion canal, EstadoEnvio estadoEnvio, Instant fechaEnvio) {
}

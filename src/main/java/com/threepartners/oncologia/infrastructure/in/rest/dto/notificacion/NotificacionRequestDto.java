package com.threepartners.oncologia.infrastructure.in.rest.dto.notificacion;

import com.threepartners.oncologia.domain.notificacion.CanalNotificacion;
import com.threepartners.oncologia.domain.notificacion.EstadoEnvio;
import jakarta.validation.constraints.NotNull;

public record NotificacionRequestDto(
        @NotNull(message = "La cita es obligatoria")
        Long citaId,

        @NotNull(message = "El canal es obligatorio")
        CanalNotificacion canal,

        @NotNull(message = "El estado de envio es obligatorio")
        EstadoEnvio estadoEnvio
) {
}

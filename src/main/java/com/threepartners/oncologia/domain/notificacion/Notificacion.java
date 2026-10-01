package com.threepartners.oncologia.domain.notificacion;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notificacion {

    private Long id;
    private Long citaId;
    private CanalNotificacion canal;
    private EstadoEnvio estadoEnvio;
    private Instant fechaEnvio;
}

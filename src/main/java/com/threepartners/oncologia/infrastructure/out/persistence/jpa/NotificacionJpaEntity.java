package com.threepartners.oncologia.infrastructure.out.persistence.jpa;

import com.threepartners.oncologia.domain.notificacion.CanalNotificacion;
import com.threepartners.oncologia.domain.notificacion.EstadoEnvio;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "notificacion")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificacionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cita_id", nullable = false)
    private Long citaId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private CanalNotificacion canal;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_envio", nullable = false, length = 15)
    private EstadoEnvio estadoEnvio;

    @Column(name = "fecha_envio", nullable = false)
    private Instant fechaEnvio;
}

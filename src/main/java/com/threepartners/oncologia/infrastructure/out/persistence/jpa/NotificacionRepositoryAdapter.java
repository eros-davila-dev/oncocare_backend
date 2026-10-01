package com.threepartners.oncologia.infrastructure.out.persistence.jpa;

import com.threepartners.oncologia.domain.notificacion.Notificacion;
import com.threepartners.oncologia.domain.notificacion.NotificacionRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificacionRepositoryAdapter implements NotificacionRepositoryPort {

    private final NotificacionJpaRepository jpaRepository;

    @Override
    public Notificacion guardar(Notificacion notificacion) {
        NotificacionJpaEntity entidad = NotificacionJpaEntity.builder()
                .id(notificacion.getId())
                .citaId(notificacion.getCitaId())
                .canal(notificacion.getCanal())
                .estadoEnvio(notificacion.getEstadoEnvio())
                .fechaEnvio(notificacion.getFechaEnvio())
                .build();

        NotificacionJpaEntity guardada = jpaRepository.save(entidad);

        return Notificacion.builder()
                .id(guardada.getId())
                .citaId(guardada.getCitaId())
                .canal(guardada.getCanal())
                .estadoEnvio(guardada.getEstadoEnvio())
                .fechaEnvio(guardada.getFechaEnvio())
                .build();
    }
}

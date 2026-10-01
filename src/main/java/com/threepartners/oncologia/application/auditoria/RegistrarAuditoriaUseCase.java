package com.threepartners.oncologia.application.auditoria;

import com.threepartners.oncologia.domain.auditoria.AuditoriaAccion;
import com.threepartners.oncologia.domain.auditoria.AuditoriaRepositoryPort;
import com.threepartners.oncologia.domain.auditoria.event.AuditoriaEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Invocado unicamente por AuditoriaEventListener (infrastructure/in/event),
 * nunca directamente por los casos de uso de negocio.
 */
@Service
@RequiredArgsConstructor
public class RegistrarAuditoriaUseCase {

    private final AuditoriaRepositoryPort auditoriaRepositoryPort;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void ejecutar(AuditoriaEvent evento) {
        AuditoriaAccion accion = AuditoriaAccion.builder()
                .usuarioId(evento.usuarioId())
                .accion(evento.accion())
                .entidadAfectada(evento.entidadAfectada())
                .entidadId(evento.entidadId())
                .valoresPrevios(evento.valoresPrevios())
                .valoresNuevos(evento.valoresNuevos())
                .ipOrigen(evento.ipOrigen())
                .fecha(Instant.now())
                .resultado(evento.resultado())
                .build();

        auditoriaRepositoryPort.registrar(accion);
    }
}

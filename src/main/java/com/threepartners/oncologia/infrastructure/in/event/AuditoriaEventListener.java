package com.threepartners.oncologia.infrastructure.in.event;

import com.threepartners.oncologia.application.auditoria.RegistrarAuditoriaUseCase;
import com.threepartners.oncologia.domain.auditoria.event.AuditoriaEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Escucha cualquier AuditoriaEvent publicado por los casos de uso de negocio y
 * lo persiste de forma asincrona, despues de que la transaccion principal haya
 * confirmado (AFTER_COMMIT). fallbackExecution=true cubre el caso de eventos
 * publicados fuera de una transaccion (ej. login fallido). Un fallo aqui jamas
 * revierte ni bloquea la operacion de negocio que lo origino.
 */
@Slf4j
@org.springframework.stereotype.Component
@RequiredArgsConstructor
public class AuditoriaEventListener {

    private final RegistrarAuditoriaUseCase registrarAuditoriaUseCase;

    @Async("auditoriaTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void alRecibir(AuditoriaEvent evento) {
        try {
            registrarAuditoriaUseCase.ejecutar(evento);
        } catch (Exception ex) {
            log.error("No se pudo registrar el evento de auditoria [{}] sobre [{}:{}]",
                    evento.accion(), evento.entidadAfectada(), evento.entidadId(), ex);
        }
    }
}

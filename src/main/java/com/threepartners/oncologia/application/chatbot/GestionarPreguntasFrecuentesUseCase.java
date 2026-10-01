package com.threepartners.oncologia.application.chatbot;

import com.threepartners.oncologia.domain.auditoria.event.OperacionAuditadaEvent;
import com.threepartners.oncologia.domain.chatbot.PreguntaFrecuente;
import com.threepartners.oncologia.domain.chatbot.PreguntaFrecuenteRepositoryPort;
import com.threepartners.oncologia.domain.shared.exception.RecursoNoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Base de conocimiento del chatbot. Lo que la fundacion cargue aqui es lo
 * unico que el bot puede afirmar sobre horarios, requisitos o ubicacion.
 */
@Service
@RequiredArgsConstructor
public class GestionarPreguntasFrecuentesUseCase {

    private final PreguntaFrecuenteRepositoryPort repositorio;
    private final ApplicationEventPublisher eventPublisher;

    /** Publico: tambien alimenta la pagina de preguntas frecuentes del portal. */
    @Transactional(readOnly = true)
    public List<PreguntaFrecuente> activas() {
        return repositorio.listarActivas();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    @Transactional(readOnly = true)
    public List<PreguntaFrecuente> todas() {
        return repositorio.listarTodas();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    @Transactional
    public PreguntaFrecuente guardar(Long id, PreguntaFrecuente datos, Long usuarioId, String ipOrigen) {
        PreguntaFrecuente pregunta = id == null ? new PreguntaFrecuente()
                : repositorio.buscarPorId(id).orElseThrow(() -> new RecursoNoEncontradoException("Pregunta frecuente", id));
        String previo = id == null ? null : "pregunta=" + pregunta.getPregunta() + ";activa=" + pregunta.isActiva();
        pregunta.setPregunta(datos.getPregunta().strip());
        pregunta.setRespuesta(datos.getRespuesta().strip());
        pregunta.setCategoria(datos.getCategoria());
        pregunta.setOrden(datos.getOrden());
        pregunta.setActiva(datos.isActiva());
        PreguntaFrecuente guardada = repositorio.guardar(pregunta);
        eventPublisher.publishEvent(OperacionAuditadaEvent.exito(usuarioId,
                id == null ? "PREGUNTA_FRECUENTE_CREADA" : "PREGUNTA_FRECUENTE_ACTUALIZADA", "PREGUNTA_FRECUENTE",
                guardada.getId(), previo, "pregunta=" + guardada.getPregunta() + ";activa=" + guardada.isActiva(), ipOrigen));
        return guardada;
    }
}

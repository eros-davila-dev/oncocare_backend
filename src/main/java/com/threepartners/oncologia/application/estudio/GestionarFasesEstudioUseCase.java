package com.threepartners.oncologia.application.estudio;

import com.threepartners.oncologia.domain.auditoria.event.OperacionAuditadaEvent;
import com.threepartners.oncologia.domain.estudio.EstadoFase;
import com.threepartners.oncologia.domain.estudio.Fase;
import com.threepartners.oncologia.domain.estudio.FaseEstudio;
import com.threepartners.oncologia.domain.estudio.FaseEstudioRepositoryPort;
import com.threepartners.oncologia.domain.shared.exception.RecursoNoEncontradoException;
import com.threepartners.oncologia.domain.shared.exception.ValidacionDeNegocioException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GestionarFasesEstudioUseCase {

    private final FaseEstudioRepositoryPort faseEstudioRepositoryPort;
    private final ApplicationEventPublisher eventPublisher;

    @PreAuthorize("hasAnyRole('ADMIN', 'INVESTIGADOR', 'MEDICO', 'RECEPCIONISTA')")
    @Transactional(readOnly = true)
    public List<FaseEstudio> listar() {
        return faseEstudioRepositoryPort.listar();
    }

    /**
     * El pretest debe terminar antes de que empiece el postest: si se
     * solaparan, un mismo evento podria contarse en ambas fases.
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'INVESTIGADOR')")
    @Transactional
    public FaseEstudio configurar(Fase fase, LocalDate inicio, LocalDate fin, Long usuarioId, String ipOrigen) {
        FaseEstudio existente = faseEstudioRepositoryPort.buscarPorFase(fase)
                .orElseGet(() -> FaseEstudio.builder().fase(fase).estado(EstadoFase.ABIERTA).build());
        String previo = existente.getFechaInicio() != null
                ? "inicio=%s;fin=%s".formatted(existente.getFechaInicio(), existente.getFechaFin())
                : null;

        existente.configurarFechas(inicio, fin);
        validarQueNoSeSolapan(existente);

        FaseEstudio guardada = faseEstudioRepositoryPort.guardar(existente);
        eventPublisher.publishEvent(OperacionAuditadaEvent.exito(usuarioId, "ESTUDIO_FASE_CONFIGURADA",
                "ESTUDIO_FASE", fase, previo, "inicio=%s;fin=%s".formatted(inicio, fin), ipOrigen));
        return guardada;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'INVESTIGADOR')")
    @Transactional
    public FaseEstudio cerrar(Fase fase, Long usuarioId, String ipOrigen) {
        FaseEstudio existente = faseEstudioRepositoryPort.buscarPorFase(fase)
                .orElseThrow(() -> new RecursoNoEncontradoException("Fase del estudio", fase.name()));
        existente.cerrar();
        FaseEstudio guardada = faseEstudioRepositoryPort.guardar(existente);
        eventPublisher.publishEvent(OperacionAuditadaEvent.exito(usuarioId, "ESTUDIO_FASE_CERRADA",
                "ESTUDIO_FASE", fase, "estado=ABIERTA", "estado=CERRADA", ipOrigen));
        return guardada;
    }

    private void validarQueNoSeSolapan(FaseEstudio cambiada) {
        Fase otra = cambiada.getFase() == Fase.PRETEST ? Fase.POSTEST : Fase.PRETEST;
        faseEstudioRepositoryPort.buscarPorFase(otra).ifPresent(f -> {
            FaseEstudio pretest = cambiada.getFase() == Fase.PRETEST ? cambiada : f;
            FaseEstudio postest = cambiada.getFase() == Fase.POSTEST ? cambiada : f;
            if (!pretest.getFechaFin().isBefore(postest.getFechaInicio())) {
                throw new ValidacionDeNegocioException(
                        "El pretest debe terminar antes de que empiece el postest (pretest hasta %s, postest desde %s)"
                                .formatted(pretest.getFechaFin(), postest.getFechaInicio()));
            }
        });
    }
}

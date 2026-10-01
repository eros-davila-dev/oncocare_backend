package com.threepartners.oncologia.application.chatbot;

import com.threepartners.oncologia.domain.auditoria.event.OperacionAuditadaEvent;
import com.threepartners.oncologia.domain.chatbot.ConversacionChatbot;
import com.threepartners.oncologia.domain.chatbot.ConversacionChatbotRepositoryPort;
import com.threepartners.oncologia.domain.estudio.Consulta;
import com.threepartners.oncologia.domain.estudio.ConsultaRepositoryPort;
import com.threepartners.oncologia.domain.shared.CriterioPaginacion;
import com.threepartners.oncologia.domain.shared.Pagina;
import com.threepartners.oncologia.domain.shared.exception.RecursoNoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

/**
 * Bandeja del personal: consultas que el chatbot no pudo resolver. Lo que el
 * personal resuelva aqui cuenta como RESUELTA_PERSONAL en el NCA.
 */
@Service
@RequiredArgsConstructor
public class BandejaConsultasUseCase {

    private final ConsultaRepositoryPort consultaRepositoryPort;
    private final ConversacionChatbotRepositoryPort conversacionRepositoryPort;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA', 'MEDICO')")
    @Transactional(readOnly = true)
    public Pagina<Consulta> escaladas(CriterioPaginacion criterio) {
        return consultaRepositoryPort.listarEscaladas(criterio);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA', 'MEDICO')")
    @Transactional(readOnly = true)
    public List<ConversacionChatbot> conversacion(Long consultaId) {
        consultaRepositoryPort.buscarPorId(consultaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Consulta", consultaId));
        return conversacionRepositoryPort.listarPorConsulta(consultaId);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA', 'MEDICO')")
    @Transactional
    public Consulta resolver(Long consultaId, boolean resuelta, String nota, Long usuarioId, String ipOrigen) {
        Consulta consulta = consultaRepositoryPort.buscarPorId(consultaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Consulta", consultaId));
        consulta.resolverPorPersonal(resuelta, usuarioId, nota, clock.instant());
        Consulta guardada = consultaRepositoryPort.guardar(consulta);
        eventPublisher.publishEvent(OperacionAuditadaEvent.exito(usuarioId, "CONSULTA_ATENDIDA_POR_PERSONAL", "CONSULTA",
                consultaId, "resultado=ESCALADA", "resultado=" + guardada.getResultado(), ipOrigen));
        return guardada;
    }
}

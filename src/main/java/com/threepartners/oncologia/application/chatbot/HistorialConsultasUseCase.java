package com.threepartners.oncologia.application.chatbot;

import com.threepartners.oncologia.domain.chatbot.ConversacionChatbot;
import com.threepartners.oncologia.domain.chatbot.ConversacionChatbotRepositoryPort;
import com.threepartners.oncologia.domain.estudio.Consulta;
import com.threepartners.oncologia.domain.estudio.ConsultaRepositoryPort;
import com.threepartners.oncologia.domain.paciente.Paciente;
import com.threepartners.oncologia.domain.paciente.PacienteRepositoryPort;
import com.threepartners.oncologia.domain.shared.CriterioPaginacion;
import com.threepartners.oncologia.domain.shared.Pagina;
import com.threepartners.oncologia.domain.shared.exception.RecursoNoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * Historial de consultas: "Mis consultas" del paciente en el portal y el
 * historial de un paciente en su ficha de la intranet. La conversacion es la
 * que ya guarda el chatbot turno a turno (conversacion_chatbot); aqui solo se
 * lee.
 *
 * El paciente solo ve las suyas: una consulta de otro responde "no
 * encontrada" (no 403), para no revelar que existe.
 */
@Service
@RequiredArgsConstructor
public class HistorialConsultasUseCase {

    private final ConsultaRepositoryPort consultaRepositoryPort;
    private final ConversacionChatbotRepositoryPort conversacionRepositoryPort;
    private final PacienteRepositoryPort pacienteRepositoryPort;

    /** Sin ficha de paciente todavia (cuenta recien creada) = sin consultas. */
    @PreAuthorize("hasRole('PACIENTE')")
    @Transactional(readOnly = true)
    public Pagina<Consulta> misConsultas(Long usuarioId, CriterioPaginacion criterio) {
        return pacienteRepositoryPort.buscarPorUsuarioId(usuarioId)
                .map(p -> consultaRepositoryPort.listarPorPaciente(p.getId(), criterio))
                .orElseGet(() -> Pagina.de(List.of(), 0, criterio.numeroPagina(), criterio.tamanoPagina()));
    }

    @PreAuthorize("hasRole('PACIENTE')")
    @Transactional(readOnly = true)
    public List<ConversacionChatbot> miConversacion(Long usuarioId, Long consultaId) {
        Long pacienteId = pacienteRepositoryPort.buscarPorUsuarioId(usuarioId).map(Paciente::getId).orElse(null);
        Consulta consulta = consultaRepositoryPort.buscarPorId(consultaId)
                .filter(c -> pacienteId != null && Objects.equals(c.getPacienteId(), pacienteId))
                .orElseThrow(() -> new RecursoNoEncontradoException("Consulta", consultaId));
        return conversacionRepositoryPort.listarPorConsulta(consulta.getId());
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'RECEPCIONISTA')")
    @Transactional(readOnly = true)
    public Pagina<Consulta> dePaciente(Long pacienteId, CriterioPaginacion criterio) {
        pacienteRepositoryPort.buscarPorId(pacienteId).orElseThrow(() -> new RecursoNoEncontradoException("Paciente", pacienteId));
        return consultaRepositoryPort.listarPorPaciente(pacienteId, criterio);
    }
}

package com.threepartners.oncologia.application.cita;

import com.threepartners.oncologia.domain.auditoria.event.OperacionAuditadaEvent;
import com.threepartners.oncologia.domain.cita.Cita;
import com.threepartners.oncologia.domain.cita.CitaRepositoryPort;
import com.threepartners.oncologia.domain.cita.EstadoCita;
import com.threepartners.oncologia.domain.paciente.Paciente;
import com.threepartners.oncologia.domain.shared.CriterioPaginacion;
import com.threepartners.oncologia.domain.shared.ZonaHoraria;
import com.threepartners.oncologia.domain.shared.exception.RecursoNoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Acciones de un paciente sobre sus propias citas cuando la identidad no
 * viene de un JWT sino de un canal ya verificado (su chat de Telegram
 * vinculado, autenticado por el secreto del webhook de n8n). Por eso no usa
 * {@code @PreAuthorize}: la autorizacion es la verificacion explicita de que
 * la cita pertenece al paciente, y todo queda auditado.
 */
@Service
@RequiredArgsConstructor
public class AccionesCitaPacienteService {

    private static final String IP_CANAL = "telegram";

    private final CitaRepositoryPort citaRepositoryPort;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public Cita citaDelPaciente(Long citaId, Paciente paciente) {
        return citaRepositoryPort.buscarPorId(citaId)
                .filter(c -> c.getPacienteId().equals(paciente.getId()))
                .orElseThrow(() -> new RecursoNoEncontradoException("Cita", citaId));
    }

    public List<Cita> proximasActivas(Paciente paciente) {
        return citaRepositoryPort.listar(paciente.getId(), null, LocalDate.now(clock.withZone(ZonaHoraria.LIMA)), null, null,
                        CriterioPaginacion.de(0, 20))
                .contenido().stream()
                .filter(c -> c.getEstado() == EstadoCita.PROGRAMADA || c.getEstado() == EstadoCita.CONFIRMADA)
                .sorted(Comparator.comparing(Cita::fechaHora))
                .toList();
    }

    public Optional<Cita> proximaActiva(Paciente paciente) {
        return proximasActivas(paciente).stream().findFirst();
    }

    public Cita confirmar(Cita cita, Paciente paciente) {
        cita.confirmar();
        Cita guardada = citaRepositoryPort.guardar(cita);
        auditar(paciente, "CITA_CONFIRMADA_POR_PACIENTE", cita, "estado=CONFIRMADA");
        return guardada;
    }

    public Cita cancelar(Cita cita, Paciente paciente, String motivo) {
        String previo = "estado=" + cita.getEstado();
        cita.cancelar();
        cita.setObservaciones(motivo);
        Cita guardada = citaRepositoryPort.guardar(cita);
        eventPublisher.publishEvent(OperacionAuditadaEvent.exito(paciente.getUsuarioId(), "CITA_CANCELADA", "CITA",
                cita.getId(), previo, "estado=CANCELADA;canal=telegram", IP_CANAL));
        return guardada;
    }

    private void auditar(Paciente paciente, String accion, Cita cita, String valores) {
        eventPublisher.publishEvent(OperacionAuditadaEvent.exito(paciente.getUsuarioId(), accion, "CITA", cita.getId(),
                null, valores + ";canal=telegram", IP_CANAL));
    }
}

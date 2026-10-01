package com.threepartners.oncologia.application.estudio;

import com.threepartners.oncologia.application.estudio.FichasEstudio.FichaAsistencia;
import com.threepartners.oncologia.application.estudio.FichasEstudio.FichaConsulta;
import com.threepartners.oncologia.application.estudio.FichasEstudio.FichaTiempo;
import com.threepartners.oncologia.domain.auditoria.event.OperacionAuditadaEvent;
import com.threepartners.oncologia.domain.cita.Cita;
import com.threepartners.oncologia.domain.cita.CitaRepositoryPort;
import com.threepartners.oncologia.domain.estudio.Consulta;
import com.threepartners.oncologia.domain.estudio.ConsultaRepositoryPort;
import com.threepartners.oncologia.domain.estudio.MedicionRegistro;
import com.threepartners.oncologia.domain.estudio.MedicionRegistroRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Captura individual (formulario) de las fichas del Anexo 2.
 */
@Service
@RequiredArgsConstructor
public class CapturarFichaEstudioUseCase {

    private final PreparadorFichasEstudio preparador;
    private final MedicionRegistroRepositoryPort medicionRegistroRepositoryPort;
    private final CitaRepositoryPort citaRepositoryPort;
    private final ConsultaRepositoryPort consultaRepositoryPort;
    private final ApplicationEventPublisher eventPublisher;

    @PreAuthorize("hasAnyRole('ADMIN', 'INVESTIGADOR')")
    @Transactional
    public MedicionRegistro registrarTiempo(FichaTiempo ficha, Long investigadorId, String ipOrigen) {
        MedicionRegistro guardada = medicionRegistroRepositoryPort.guardar(preparador.prepararTiempo(ficha, investigadorId));
        auditar(investigadorId, "ESTUDIO_FICHA_TIEMPO_CAPTURADA", "MEDICION_REGISTRO", guardada.getId(),
                "paciente=%d;segundos=%d".formatted(ficha.pacienteId(), guardada.duracion().toSeconds()), ipOrigen);
        return guardada;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'INVESTIGADOR')")
    @Transactional
    public Cita registrarAsistencia(FichaAsistencia ficha, Long investigadorId, String ipOrigen) {
        Cita guardada = citaRepositoryPort.guardar(preparador.prepararAsistencia(ficha, investigadorId));
        auditar(investigadorId, "ESTUDIO_FICHA_ASISTENCIA_CAPTURADA", "CITA", guardada.getId(),
                "paciente=%d;fecha=%s;estado=%s".formatted(ficha.pacienteId(), ficha.fecha(), guardada.getEstado()), ipOrigen);
        return guardada;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'INVESTIGADOR')")
    @Transactional
    public Consulta registrarConsulta(FichaConsulta ficha, Long investigadorId, String ipOrigen) {
        Consulta guardada = consultaRepositoryPort.guardar(preparador.prepararConsulta(ficha, investigadorId));
        auditar(investigadorId, "ESTUDIO_FICHA_CONSULTA_CAPTURADA", "CONSULTA", guardada.getId(),
                "canal=%s;resultado=%s".formatted(guardada.getCanal(), guardada.getResultado()), ipOrigen);
        return guardada;
    }

    private void auditar(Long usuarioId, String accion, String entidad, Long id, String valores, String ip) {
        eventPublisher.publishEvent(OperacionAuditadaEvent.exito(usuarioId, accion, entidad, id, null, valores, ip));
    }
}

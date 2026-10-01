package com.threepartners.oncologia.application.estudio;

import com.threepartners.oncologia.domain.auditoria.event.OperacionAuditadaEvent;
import com.threepartners.oncologia.domain.estudio.Consulta;
import com.threepartners.oncologia.domain.estudio.ConsultaRepositoryPort;
import com.threepartners.oncologia.domain.estudio.CorreccionMedicion;
import com.threepartners.oncologia.domain.estudio.CorreccionMedicion.EntidadCorregida;
import com.threepartners.oncologia.domain.estudio.CorreccionMedicionRepositoryPort;
import com.threepartners.oncologia.domain.estudio.MedicionRegistro;
import com.threepartners.oncologia.domain.estudio.MedicionRegistroRepositoryPort;
import com.threepartners.oncologia.domain.shared.exception.RecursoNoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * Unica forma de sacar un dato del calculo de un indicador: anularlo con un
 * motivo. El dato sigue en la base (la tabla no admite DELETE) y queda el
 * rastro en correccion_medicion y en la auditoria.
 */
@Service
@RequiredArgsConstructor
public class AnularDatoEstudioUseCase {

    private final MedicionRegistroRepositoryPort medicionRegistroRepositoryPort;
    private final ConsultaRepositoryPort consultaRepositoryPort;
    private final CorreccionMedicionRepositoryPort correccionRepositoryPort;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @PreAuthorize("hasAnyRole('ADMIN', 'INVESTIGADOR')")
    @Transactional
    public MedicionRegistro anularMedicion(Long medicionId, String motivo, Long usuarioId, String ipOrigen) {
        MedicionRegistro medicion = medicionRegistroRepositoryPort.buscarPorId(medicionId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Medicion de registro", medicionId));
        String previo = medicion.getEstado().name();
        registrarCorreccion(EntidadCorregida.MEDICION_REGISTRO, medicionId, "estado", previo, motivo, usuarioId);
        medicion.anular();
        MedicionRegistro guardada = medicionRegistroRepositoryPort.guardar(medicion);
        eventPublisher.publishEvent(OperacionAuditadaEvent.exito(usuarioId, "ESTUDIO_MEDICION_ANULADA",
                "MEDICION_REGISTRO", medicionId, "estado=" + previo, "estado=ANULADA;motivo=" + motivo, ipOrigen));
        return guardada;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'INVESTIGADOR')")
    @Transactional
    public Consulta anularConsulta(Long consultaId, String motivo, Long usuarioId, String ipOrigen) {
        Consulta consulta = consultaRepositoryPort.buscarPorId(consultaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Consulta", consultaId));
        String previo = String.valueOf(consulta.getResultado());
        registrarCorreccion(EntidadCorregida.CONSULTA, consultaId, "resultado", previo, motivo, usuarioId);
        consulta.anular();
        Consulta guardada = consultaRepositoryPort.guardar(consulta);
        eventPublisher.publishEvent(OperacionAuditadaEvent.exito(usuarioId, "ESTUDIO_CONSULTA_ANULADA",
                "CONSULTA", consultaId, "resultado=" + previo, "resultado=ANULADA;motivo=" + motivo, ipOrigen));
        return guardada;
    }

    private void registrarCorreccion(EntidadCorregida entidad, Long id, String campo, String previo,
                                     String motivo, Long usuarioId) {
        correccionRepositoryPort.guardar(new CorreccionMedicion(null, entidad, id, campo, previo, "ANULADA",
                motivo, usuarioId, clock.instant()));
    }
}

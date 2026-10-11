package com.threepartners.oncologia.application.dashboard;

import com.threepartners.oncologia.application.estudio.PeriodosEstudioService;
import com.threepartners.oncologia.domain.auditoria.event.OperacionAuditadaEvent;
import com.threepartners.oncologia.domain.dashboard.ActividadPeriodo;
import com.threepartners.oncologia.domain.dashboard.ActividadRepositoryPort;
import com.threepartners.oncologia.domain.dashboard.DatosDemoPort;
import com.threepartners.oncologia.domain.estudio.EventosImportadosRepositoryPort;
import com.threepartners.oncologia.domain.estudio.PeriodoMedicion;
import com.threepartners.oncologia.domain.shared.ZonaHoraria;
import com.threepartners.oncologia.domain.shared.exception.ValidacionDeNegocioException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Panel del personal: todo lo que se hizo en el periodo (registros con su
 * tiempo, citas y ausentismo, recordatorios, consultas atendidas por el
 * asistente), dia por dia y fila por fila.
 *
 * El resumen lo ven todos los roles del panel; las filas llevan nombres de
 * pacientes, asi que el detalle y la descarga quedan para el personal que
 * atiende (el investigador trabaja con datos anonimos).
 */
@Service
@RequiredArgsConstructor
public class ConsultarActividadUseCase {

    /** Un periodo mas largo haria pesada la pantalla; para eso esta la descarga por tramos. */
    private static final long MAXIMO_DIAS = 366;

    private final ActividadRepositoryPort repositorio;
    private final DatosDemoPort datosDemoPort;
    private final EventosImportadosRepositoryPort eventosImportados;
    private final PeriodosEstudioService periodosEstudioService;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    /**
     * De donde salen los datos del panel: LOCAL = lo que mide esta instalacion;
     * SERVIDOR = eventos importados del servidor de la fundacion (y las fichas
     * del pretest), calculados con las mismas reglas.
     */
    public enum Fuente { LOCAL, SERVIDOR }

    public record Exportacion(Instant generadoEn, ActividadPeriodo.Resumen resumen, ActividadPeriodo.Filas filas) {
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'RECEPCIONISTA', 'INVESTIGADOR')")
    @Transactional(readOnly = true)
    public ActividadPeriodo.Resumen resumen(LocalDate desde, LocalDate hasta) {
        return resumen(desde, hasta, Fuente.LOCAL);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'RECEPCIONISTA', 'INVESTIGADOR')")
    @Transactional(readOnly = true)
    public ActividadPeriodo.Resumen resumen(LocalDate desde, LocalDate hasta, Fuente fuente) {
        PeriodoMedicion periodo = periodo(desde, hasta);
        return ActividadPeriodo.resumir(periodo.desde(), periodo.hasta(), filas(periodo, fuente), hoy());
    }

    private ActividadPeriodo.Filas filas(PeriodoMedicion periodo, Fuente fuente) {
        return fuente == Fuente.SERVIDOR
                ? ActividadDesdeEventosImportados.filas(eventosImportados.listarEntre(periodo.desde(), periodo.hasta()))
                : repositorio.filas(periodo);
    }

    /**
     * Pacientes de demostracion activos: mientras existan, el panel avisa que
     * parte de lo que muestra es ficticio (docker/demo/).
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'RECEPCIONISTA', 'INVESTIGADOR')")
    @Transactional(readOnly = true)
    public int pacientesDemo() {
        return datosDemoPort.pacientesDemoActivos();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'RECEPCIONISTA')")
    @Transactional(readOnly = true)
    public ActividadPeriodo.Filas detalle(LocalDate desde, LocalDate hasta) {
        return detalle(desde, hasta, Fuente.LOCAL);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'RECEPCIONISTA')")
    @Transactional(readOnly = true)
    public ActividadPeriodo.Filas detalle(LocalDate desde, LocalDate hasta, Fuente fuente) {
        return filas(periodo(desde, hasta), fuente);
    }

    /** Descarga auditada: lleva nombres de pacientes. */
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    @Transactional(readOnly = true)
    public Exportacion exportar(LocalDate desde, LocalDate hasta, Long usuarioId, String ipOrigen) {
        return exportar(desde, hasta, Fuente.LOCAL, usuarioId, ipOrigen);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    @Transactional(readOnly = true)
    public Exportacion exportar(LocalDate desde, LocalDate hasta, Fuente fuente, Long usuarioId, String ipOrigen) {
        PeriodoMedicion periodo = periodo(desde, hasta);
        var filas = filas(periodo, fuente);
        var resumen = ActividadPeriodo.resumir(periodo.desde(), periodo.hasta(), filas, hoy());
        eventPublisher.publishEvent(OperacionAuditadaEvent.exito(usuarioId, "DASHBOARD_EXPORTACION", "DASHBOARD", null,
                null, "fuente=" + fuente + ";desde=%s;hasta=%s;registros=%d;citas=%d;consultas=%d".formatted(periodo.desde(), periodo.hasta(),
                        filas.registros().size(), filas.citas().size(), filas.consultas().size()),
                ipOrigen));
        return new Exportacion(clock.instant(), resumen, filas);
    }

    private PeriodoMedicion periodo(LocalDate desde, LocalDate hasta) {
        PeriodoMedicion periodo = periodosEstudioService.resolver(null, desde, hasta, hoy());
        if (ChronoUnit.DAYS.between(periodo.desde(), periodo.hasta()) > MAXIMO_DIAS) {
            throw new ValidacionDeNegocioException("Elija un periodo de hasta un año");
        }
        return periodo;
    }

    private LocalDate hoy() {
        return LocalDate.now(clock.withZone(ZonaHoraria.LIMA));
    }
}

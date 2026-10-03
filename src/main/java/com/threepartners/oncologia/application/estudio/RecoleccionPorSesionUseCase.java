package com.threepartners.oncologia.application.estudio;

import com.threepartners.oncologia.config.EstudioProperties;
import com.threepartners.oncologia.domain.auditoria.event.OperacionAuditadaEvent;
import com.threepartners.oncologia.domain.estudio.Fase;
import com.threepartners.oncologia.domain.estudio.PeriodoMedicion;
import com.threepartners.oncologia.domain.estudio.RecoleccionEstudioRepositoryPort;
import com.threepartners.oncologia.domain.estudio.RecoleccionSesiones;
import com.threepartners.oncologia.domain.shared.ZonaHoraria;
import com.threepartners.oncologia.domain.shared.exception.ValidacionDeNegocioException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

/**
 * Recoleccion de datos por sesion (tesis v8): el panel por sesion de una fase,
 * el detalle evento por evento (las tres fichas del Anexo 2) y su
 * exportacion a Excel con las columnas del Instrumento de la tesis.
 */
@Service
@RequiredArgsConstructor
public class RecoleccionPorSesionUseCase {

    private final RecoleccionEstudioRepositoryPort repositorio;
    private final PeriodosEstudioService periodosEstudioService;
    private final EstudioProperties estudioProperties;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public record Exportacion(Instant generadoEn, RecoleccionSesiones.Resumen resumen, RecoleccionSesiones.Detalle detalle) {
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'INVESTIGADOR')")
    @Transactional(readOnly = true)
    public RecoleccionSesiones.Resumen resumen(Fase fase) {
        PeriodoMedicion periodo = periodosEstudioService.periodoDe(fase);
        return RecoleccionSesiones.resumir(fase, periodo.desde(), periodo.hasta(), estudioProperties.diasDeSesion(),
                repositorio.filas(periodo), hoy());
    }

    /** Fichas de una sesion (fecha) o de todas las sesiones de la fase (fecha null). */
    @PreAuthorize("hasAnyRole('ADMIN', 'INVESTIGADOR')")
    @Transactional(readOnly = true)
    public RecoleccionSesiones.Detalle detalle(Fase fase, LocalDate fecha) {
        PeriodoMedicion periodo = periodosEstudioService.periodoDe(fase);
        Set<LocalDate> fechas = Set.copyOf(RecoleccionSesiones.fechasDeSesion(
                periodo.desde(), periodo.hasta(), estudioProperties.diasDeSesion()));
        if (fecha != null) {
            if (!fechas.contains(fecha)) {
                throw new ValidacionDeNegocioException("El " + fecha + " no es un dia de sesion de la fase " + fase);
            }
            return RecoleccionSesiones.filtrar(repositorio.filas(new PeriodoMedicion(fecha, fecha)), Set.of(fecha));
        }
        return RecoleccionSesiones.filtrar(repositorio.filas(periodo), fechas);
    }

    /** Descarga auditada: son datos de pacientes reales, aunque anonimos. */
    @PreAuthorize("hasAnyRole('ADMIN', 'INVESTIGADOR')")
    @Transactional(readOnly = true)
    public Exportacion exportar(Fase fase, Long usuarioId, String ipOrigen) {
        PeriodoMedicion periodo = periodosEstudioService.periodoDe(fase);
        Set<DayOfWeek> dias = estudioProperties.diasDeSesion();
        RecoleccionSesiones.Detalle todo = repositorio.filas(periodo);
        RecoleccionSesiones.Resumen resumen = RecoleccionSesiones.resumir(fase, periodo.desde(), periodo.hasta(), dias, todo, hoy());
        RecoleccionSesiones.Detalle enSesion = RecoleccionSesiones.filtrar(todo,
                Set.copyOf(RecoleccionSesiones.fechasDeSesion(periodo.desde(), periodo.hasta(), dias)));
        eventPublisher.publishEvent(OperacionAuditadaEvent.exito(usuarioId, "ESTUDIO_EXPORTACION_RECOLECCION",
                "ESTUDIO", null, null, "fase=%s;sesiones=%d;registros=%d;citas=%d;consultas=%d".formatted(
                        fase, resumen.sesiones().size(), resumen.registros(), resumen.citasElegibles(), resumen.consultas()),
                ipOrigen));
        return new Exportacion(clock.instant(), resumen, enSesion);
    }

    private LocalDate hoy() {
        return LocalDate.now(clock.withZone(ZonaHoraria.LIMA));
    }
}

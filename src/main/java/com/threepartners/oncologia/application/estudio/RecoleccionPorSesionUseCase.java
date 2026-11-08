package com.threepartners.oncologia.application.estudio;

import com.threepartners.oncologia.application.dashboard.ActividadDesdeEventosImportados;
import com.threepartners.oncologia.config.EstudioProperties;
import com.threepartners.oncologia.domain.auditoria.event.OperacionAuditadaEvent;
import com.threepartners.oncologia.domain.estudio.EventoImportado;
import com.threepartners.oncologia.domain.estudio.EventosImportadosRepositoryPort;
import com.threepartners.oncologia.domain.estudio.Fase;
import com.threepartners.oncologia.domain.estudio.PeriodoMedicion;
import com.threepartners.oncologia.domain.estudio.RecoleccionEstudioRepositoryPort;
import com.threepartners.oncologia.domain.estudio.RecoleccionSesiones;
import com.threepartners.oncologia.domain.estudio.TipoEventoImportado;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Recoleccion de datos por sesion (tesis v8): el panel por sesion de una fase,
 * el detalle evento por evento (las tres fichas del Anexo 2) y su
 * exportacion a Excel con las columnas del Instrumento de la tesis.
 *
 * Dos fuentes, con el mismo calculo (RecoleccionSesiones): LOCAL = lo que mide
 * esta instalacion en el periodo configurado de la fase; SERVIDOR = los eventos
 * importados del servidor de la fundacion (y las fichas del pretest), en el
 * rango de fechas de esos eventos.
 */
@Service
@RequiredArgsConstructor
public class RecoleccionPorSesionUseCase {

    public enum Fuente { LOCAL, SERVIDOR }

    private final RecoleccionEstudioRepositoryPort repositorio;
    private final EventosImportadosRepositoryPort eventosImportados;
    private final PeriodosEstudioService periodosEstudioService;
    private final EstudioProperties estudioProperties;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public record Exportacion(Instant generadoEn, RecoleccionSesiones.Resumen resumen, RecoleccionSesiones.Detalle detalle) {
    }

    /** Periodo y filas de una fase segun la fuente. */
    private record Origen(PeriodoMedicion periodo, RecoleccionSesiones.Detalle filas) {
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'INVESTIGADOR')")
    @Transactional(readOnly = true)
    public RecoleccionSesiones.Resumen resumen(Fase fase) {
        return resumen(fase, Fuente.LOCAL);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'INVESTIGADOR')")
    @Transactional(readOnly = true)
    public RecoleccionSesiones.Resumen resumen(Fase fase, Fuente fuente) {
        Origen origen = origen(fase, fuente);
        return RecoleccionSesiones.resumir(fase, origen.periodo().desde(), origen.periodo().hasta(),
                estudioProperties.diasDeSesion(), origen.filas(), hoy());
    }

    /** Fichas de una sesion (fecha) o de todas las sesiones de la fase (fecha null). */
    @PreAuthorize("hasAnyRole('ADMIN', 'INVESTIGADOR')")
    @Transactional(readOnly = true)
    public RecoleccionSesiones.Detalle detalle(Fase fase, LocalDate fecha) {
        return detalle(fase, fecha, Fuente.LOCAL);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'INVESTIGADOR')")
    @Transactional(readOnly = true)
    public RecoleccionSesiones.Detalle detalle(Fase fase, LocalDate fecha, Fuente fuente) {
        Origen origen = origen(fase, fuente);
        Set<LocalDate> fechas = Set.copyOf(RecoleccionSesiones.fechasDeSesion(
                origen.periodo().desde(), origen.periodo().hasta(), estudioProperties.diasDeSesion()));
        if (fecha != null) {
            if (!fechas.contains(fecha)) {
                throw new ValidacionDeNegocioException("El " + fecha + " no es un dia de sesion de la fase " + fase);
            }
            return RecoleccionSesiones.filtrar(origen.filas(), Set.of(fecha));
        }
        return RecoleccionSesiones.filtrar(origen.filas(), fechas);
    }

    /** Descarga auditada: son datos de pacientes reales, aunque anonimos. */
    @PreAuthorize("hasAnyRole('ADMIN', 'INVESTIGADOR')")
    @Transactional(readOnly = true)
    public Exportacion exportar(Fase fase, Long usuarioId, String ipOrigen) {
        return exportar(fase, Fuente.LOCAL, usuarioId, ipOrigen);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'INVESTIGADOR')")
    @Transactional(readOnly = true)
    public Exportacion exportar(Fase fase, Fuente fuente, Long usuarioId, String ipOrigen) {
        Origen origen = origen(fase, fuente);
        PeriodoMedicion periodo = origen.periodo();
        Set<DayOfWeek> dias = estudioProperties.diasDeSesion();
        RecoleccionSesiones.Detalle todo = origen.filas();
        RecoleccionSesiones.Resumen resumen = RecoleccionSesiones.resumir(fase, periodo.desde(), periodo.hasta(), dias, todo, hoy());
        RecoleccionSesiones.Detalle enSesion = RecoleccionSesiones.filtrar(todo,
                Set.copyOf(RecoleccionSesiones.fechasDeSesion(periodo.desde(), periodo.hasta(), dias)));
        eventPublisher.publishEvent(OperacionAuditadaEvent.exito(usuarioId, "ESTUDIO_EXPORTACION_RECOLECCION",
                "ESTUDIO", null, null, "fase=%s;fuente=%s;sesiones=%d;registros=%d;citas=%d;consultas=%d".formatted(
                        fase, fuente, resumen.sesiones().size(), resumen.registros(), resumen.citasElegibles(),
                        resumen.consultas()),
                ipOrigen));
        return new Exportacion(clock.instant(), resumen, enSesion);
    }

    private Origen origen(Fase fase, Fuente fuente) {
        if (fuente == Fuente.LOCAL) {
            PeriodoMedicion periodo = periodosEstudioService.periodoDe(fase);
            return new Origen(periodo, repositorio.filas(periodo));
        }
        List<EventoImportado> eventos = new ArrayList<>();
        for (TipoEventoImportado tipo : TipoEventoImportado.values()) {
            eventos.addAll(eventosImportados.listar(tipo, fase));
        }
        if (eventos.isEmpty()) {
            throw new ValidacionDeNegocioException("No hay eventos importados del " + fase.name().toLowerCase()
                    + " (Estudio > Fases y sesiones > Importar eventos)");
        }
        LocalDate desde = eventos.stream().map(EventoImportado::fecha).min(Comparator.naturalOrder()).orElseThrow();
        LocalDate hasta = eventos.stream().map(EventoImportado::fecha).max(Comparator.naturalOrder()).orElseThrow();
        return new Origen(new PeriodoMedicion(desde, hasta), ActividadDesdeEventosImportados.fichas(eventos));
    }

    private LocalDate hoy() {
        return LocalDate.now(clock.withZone(ZonaHoraria.LIMA));
    }
}

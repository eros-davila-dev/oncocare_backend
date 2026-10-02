package com.threepartners.oncologia.application.estudio;

import com.threepartners.oncologia.domain.auditoria.event.OperacionAuditadaEvent;
import com.threepartners.oncologia.domain.estudio.AlcanceIndicador;
import com.threepartners.oncologia.domain.estudio.AnalisisPareado;
import com.threepartners.oncologia.domain.estudio.CanalMedicion;
import com.threepartners.oncologia.domain.estudio.ComparativoIndicadores;
import com.threepartners.oncologia.domain.estudio.Fase;
import com.threepartners.oncologia.domain.estudio.FilaPareada;
import com.threepartners.oncologia.domain.estudio.FiltroIndicadores;
import com.threepartners.oncologia.domain.estudio.IndicadoresEstudio;
import com.threepartners.oncologia.domain.estudio.IndicadoresEstudioRepositoryPort;
import com.threepartners.oncologia.domain.estudio.RegistrosEstudioRepositoryPort;
import com.threepartners.oncologia.domain.estudio.RegistrosFichas;
import com.threepartners.oncologia.domain.estudio.TipoMedicion;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * Datos del estudio para analizarlos fuera del sistema (SPSS 29, como declara
 * la tesis). Todo sale anonimizado con el codigo de participante y cada
 * descarga queda en la auditoria: son datos de salud de pacientes reales.
 */
@Service
@RequiredArgsConstructor
public class ExportarDatosEstudioUseCase {

    private final ConsultarIndicadoresEstudioUseCase consultarIndicadoresUseCase;
    private final IndicadoresEstudioRepositoryPort indicadoresRepositoryPort;
    private final RegistrosEstudioRepositoryPort registrosRepositoryPort;
    private final PeriodosEstudioService periodosEstudioService;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    /** Tabla pareada + resumen + Wilcoxon preliminar, lista para abrir en SPSS. */
    @PreAuthorize("hasAnyRole('ADMIN', 'INVESTIGADOR')")
    @Transactional(readOnly = true)
    public ExportacionSpss spss(TipoMedicion tipoRegistro, Set<CanalMedicion> canales,
                                Long usuarioId, String ipOrigen) {
        List<FilaPareada> filas = consultarIndicadoresUseCase.pareado(tipoRegistro, canales);
        ComparativoIndicadores comparativo = consultarIndicadoresUseCase.comparativo(
                AlcanceIndicador.MUESTRA, tipoRegistro, canales);
        var filtro = new FiltroIndicadores(comparativo.periodoPretest(), AlcanceIndicador.MUESTRA, tipoRegistro, canales);

        eventPublisher.publishEvent(OperacionAuditadaEvent.exito(usuarioId, "ESTUDIO_EXPORTACION_SPSS",
                "ESTUDIO", null, null, "participantes=" + filas.size(), ipOrigen));
        return new ExportacionSpss(clock.instant(), filtro.tipoRegistro(), filtro.canalesRegistro(),
                filas, comparativo, AnalisisPareado.de(filas));
    }

    /** Las tres fichas del Anexo 2 de una fase, fila por fila. */
    @PreAuthorize("hasAnyRole('ADMIN', 'INVESTIGADOR')")
    @Transactional(readOnly = true)
    public ExportacionFichas fichas(Fase fase, AlcanceIndicador alcance, TipoMedicion tipoRegistro,
                                    Set<CanalMedicion> canales, Long usuarioId, String ipOrigen) {
        var filtro = new FiltroIndicadores(periodosEstudioService.periodoDe(fase), alcance, tipoRegistro, canales);
        var exportacion = new ExportacionFichas(clock.instant(), fase, filtro,
                IndicadoresEstudio.de(indicadoresRepositoryPort.contar(filtro)),
                registrosRepositoryPort.tiempos(filtro),
                registrosRepositoryPort.asistencias(filtro),
                registrosRepositoryPort.consultas(filtro));

        eventPublisher.publishEvent(OperacionAuditadaEvent.exito(usuarioId, "ESTUDIO_EXPORTACION_FICHAS",
                "ESTUDIO", null, null, "fase=" + fase + ";alcance=" + filtro.alcance(), ipOrigen));
        return exportacion;
    }

    public record ExportacionSpss(
            Instant generadoEn,
            TipoMedicion tipoRegistro,
            Set<CanalMedicion> canalesRegistro,
            List<FilaPareada> filas,
            ComparativoIndicadores comparativo,
            AnalisisPareado analisis
    ) {
    }

    /**
     * Incluye los indicadores calculados con el mismo filtro: el investigador
     * puede comprobar en la hoja que las filas suman lo que muestra el sistema.
     */
    public record ExportacionFichas(
            Instant generadoEn,
            Fase fase,
            FiltroIndicadores filtro,
            IndicadoresEstudio indicadores,
            List<RegistrosFichas.Tiempo> tiempos,
            List<RegistrosFichas.Asistencia> asistencias,
            List<RegistrosFichas.ConsultaCerrada> consultas
    ) {
    }
}

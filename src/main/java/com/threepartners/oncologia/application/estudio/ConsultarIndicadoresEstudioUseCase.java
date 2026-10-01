package com.threepartners.oncologia.application.estudio;

import com.threepartners.oncologia.domain.estudio.AlcanceIndicador;
import com.threepartners.oncologia.domain.estudio.CanalMedicion;
import com.threepartners.oncologia.domain.estudio.ComparativoIndicadores;
import com.threepartners.oncologia.domain.estudio.ConteosIndicadores;
import com.threepartners.oncologia.domain.estudio.Fase;
import com.threepartners.oncologia.domain.estudio.FilaPareada;
import com.threepartners.oncologia.domain.estudio.FiltroIndicadores;
import com.threepartners.oncologia.domain.estudio.IndicadoresEstudio;
import com.threepartners.oncologia.domain.estudio.IndicadoresEstudioRepositoryPort;
import com.threepartners.oncologia.domain.estudio.ParticipanteEstudioRepositoryPort;
import com.threepartners.oncologia.domain.estudio.ParticipanteResumen;
import com.threepartners.oncologia.domain.estudio.PeriodoMedicion;
import com.threepartners.oncologia.domain.estudio.TipoMedicion;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Indicadores de la tesis: por fase o rango, comparativo pretest/postest y
 * tabla pareada por participante (entrada de la prueba de Wilcoxon).
 */
@Service
@RequiredArgsConstructor
public class ConsultarIndicadoresEstudioUseCase {

    private final IndicadoresEstudioRepositoryPort indicadoresRepositoryPort;
    private final ParticipanteEstudioRepositoryPort participanteRepositoryPort;
    private final PeriodosEstudioService periodosEstudioService;
    private final Clock clock;

    @PreAuthorize("hasAnyRole('ADMIN', 'INVESTIGADOR', 'MEDICO', 'RECEPCIONISTA')")
    @Transactional(readOnly = true)
    public ResultadoIndicadores calcular(Fase fase, LocalDate desde, LocalDate hasta, AlcanceIndicador alcance,
                                        TipoMedicion tipoRegistro, Set<CanalMedicion> canales) {
        PeriodoMedicion periodo = periodosEstudioService.resolver(fase, desde, hasta, LocalDate.now(clock));
        var filtro = new FiltroIndicadores(periodo, alcance, tipoRegistro, canales);
        return new ResultadoIndicadores(fase, filtro, IndicadoresEstudio.de(indicadoresRepositoryPort.contar(filtro)));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'INVESTIGADOR', 'MEDICO', 'RECEPCIONISTA')")
    @Transactional(readOnly = true)
    public ComparativoIndicadores comparativo(AlcanceIndicador alcance, TipoMedicion tipoRegistro,
                                              Set<CanalMedicion> canales) {
        PeriodoMedicion pre = periodosEstudioService.periodoDe(Fase.PRETEST);
        PeriodoMedicion post = periodosEstudioService.periodoDe(Fase.POSTEST);
        var filtroPre = new FiltroIndicadores(pre, alcance, tipoRegistro, canales);
        var filtroPost = new FiltroIndicadores(post, alcance, tipoRegistro, canales);
        return ComparativoIndicadores.de(filtroPre.alcance(), pre, post,
                IndicadoresEstudio.de(indicadoresRepositoryPort.contar(filtroPre)),
                IndicadoresEstudio.de(indicadoresRepositoryPort.contar(filtroPost)));
    }

    /**
     * Una fila por participante incluido, aunque no tenga datos en alguna
     * fase (sus indicadores salen "sin datos"): asi el investigador ve de
     * inmediato a quien le falta medicion antes de cerrar una fase.
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'INVESTIGADOR')")
    @Transactional(readOnly = true)
    public List<FilaPareada> pareado(TipoMedicion tipoRegistro, Set<CanalMedicion> canales) {
        var filtroPre = new FiltroIndicadores(periodosEstudioService.periodoDe(Fase.PRETEST),
                AlcanceIndicador.MUESTRA, tipoRegistro, canales);
        var filtroPost = new FiltroIndicadores(periodosEstudioService.periodoDe(Fase.POSTEST),
                AlcanceIndicador.MUESTRA, tipoRegistro, canales);
        Map<String, ConteosIndicadores> pre = indicadoresRepositoryPort.contarPorParticipante(filtroPre);
        Map<String, ConteosIndicadores> post = indicadoresRepositoryPort.contarPorParticipante(filtroPost);

        return participanteRepositoryPort.listar().stream()
                .map(ParticipanteResumen::participante)
                .filter(p -> p.isIncluido())
                .map(p -> new FilaPareada(p.getCodigo(),
                        IndicadoresEstudio.de(pre.getOrDefault(p.getCodigo(), ConteosIndicadores.vacios())),
                        IndicadoresEstudio.de(post.getOrDefault(p.getCodigo(), ConteosIndicadores.vacios()))))
                .toList();
    }

    public record ResultadoIndicadores(Fase fase, FiltroIndicadores filtro, IndicadoresEstudio indicadores) {
    }
}

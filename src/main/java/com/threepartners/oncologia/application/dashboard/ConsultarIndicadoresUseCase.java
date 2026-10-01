package com.threepartners.oncologia.application.dashboard;

import com.threepartners.oncologia.application.estudio.PeriodosEstudioService;
import com.threepartners.oncologia.domain.dashboard.ResumenDashboard;
import com.threepartners.oncologia.domain.estudio.AlcanceIndicador;
import com.threepartners.oncologia.domain.estudio.FiltroIndicadores;
import com.threepartners.oncologia.domain.estudio.IndicadoresEstudio;
import com.threepartners.oncologia.domain.estudio.IndicadoresEstudioRepositoryPort;
import com.threepartners.oncologia.domain.estudio.PeriodoMedicion;
import com.threepartners.oncologia.domain.tratamiento.CicloTratamientoRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;

/**
 * Indicadores del dashboard de la intranet. Usa exactamente el mismo calculo
 * que el modulo de estudio (alcance GLOBAL), para que el numero que ve el
 * personal a diario y el que se reporta en la tesis nunca diverjan.
 */
@Service
@RequiredArgsConstructor
public class ConsultarIndicadoresUseCase {

    private final IndicadoresEstudioRepositoryPort indicadoresRepositoryPort;
    private final CicloTratamientoRepositoryPort cicloTratamientoRepositoryPort;
    private final PeriodosEstudioService periodosEstudioService;
    private final Clock clock;

    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'RECEPCIONISTA', 'INVESTIGADOR')")
    @Transactional(readOnly = true)
    public ResumenDashboard ejecutar(LocalDate desde, LocalDate hasta) {
        PeriodoMedicion periodo = periodosEstudioService.resolver(null, desde, hasta, LocalDate.now(clock));
        var filtro = FiltroIndicadores.deHipotesis(periodo, AlcanceIndicador.GLOBAL);
        return new ResumenDashboard(
                periodo,
                IndicadoresEstudio.de(indicadoresRepositoryPort.contar(filtro)),
                cicloTratamientoRepositoryPort.promedioCumplimiento());
    }
}

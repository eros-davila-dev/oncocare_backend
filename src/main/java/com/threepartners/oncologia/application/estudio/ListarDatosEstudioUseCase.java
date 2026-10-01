package com.threepartners.oncologia.application.estudio;

import com.threepartners.oncologia.domain.estudio.CanalConsulta;
import com.threepartners.oncologia.domain.estudio.CanalMedicion;
import com.threepartners.oncologia.domain.estudio.Consulta;
import com.threepartners.oncologia.domain.estudio.ConsultaRepositoryPort;
import com.threepartners.oncologia.domain.estudio.EstadoMedicion;
import com.threepartners.oncologia.domain.estudio.Fase;
import com.threepartners.oncologia.domain.estudio.MedicionRegistro;
import com.threepartners.oncologia.domain.estudio.MedicionRegistroRepositoryPort;
import com.threepartners.oncologia.domain.estudio.PeriodoMedicion;
import com.threepartners.oncologia.domain.estudio.ResultadoConsulta;
import com.threepartners.oncologia.domain.estudio.TipoMedicion;
import com.threepartners.oncologia.domain.shared.CriterioPaginacion;
import com.threepartners.oncologia.domain.shared.Pagina;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Detalle de los datos que alimentan los indicadores, para que el
 * investigador pueda revisarlos (y anular los erroneos) antes de cerrar una
 * fase.
 */
@Service
@RequiredArgsConstructor
public class ListarDatosEstudioUseCase {

    private final MedicionRegistroRepositoryPort medicionRegistroRepositoryPort;
    private final ConsultaRepositoryPort consultaRepositoryPort;
    private final PeriodosEstudioService periodosEstudioService;

    @PreAuthorize("hasAnyRole('ADMIN', 'INVESTIGADOR')")
    @Transactional(readOnly = true)
    public Pagina<MedicionRegistro> mediciones(Fase fase, TipoMedicion tipo, CanalMedicion canal,
                                               EstadoMedicion estado, CriterioPaginacion criterio) {
        return medicionRegistroRepositoryPort.listar(periodo(fase), tipo, canal, estado, criterio);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'INVESTIGADOR')")
    @Transactional(readOnly = true)
    public Pagina<Consulta> consultas(Fase fase, CanalConsulta canal, ResultadoConsulta resultado,
                                      CriterioPaginacion criterio) {
        return consultaRepositoryPort.listar(periodo(fase), canal, resultado, criterio);
    }

    private PeriodoMedicion periodo(Fase fase) {
        return fase != null ? periodosEstudioService.periodoDe(fase) : null;
    }
}

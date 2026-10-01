package com.threepartners.oncologia.domain.estudio;

import com.threepartners.oncologia.domain.shared.CriterioPaginacion;
import com.threepartners.oncologia.domain.shared.Pagina;

import java.util.Optional;

public interface ConsultaRepositoryPort {

    Consulta guardar(Consulta consulta);

    Optional<Consulta> buscarPorId(Long id);

    Pagina<Consulta> listar(PeriodoMedicion periodo, CanalConsulta canal, ResultadoConsulta resultado,
                            CriterioPaginacion criterio);
}

package com.threepartners.oncologia.domain.estudio;

import com.threepartners.oncologia.domain.shared.CriterioPaginacion;
import com.threepartners.oncologia.domain.shared.Pagina;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ConsultaRepositoryPort {

    Consulta guardar(Consulta consulta);

    Optional<Consulta> buscarPorId(Long id);

    /** Ultima consulta de una sesion de chat (web o Telegram), para continuar la necesidad en curso. */
    Optional<Consulta> buscarUltimaPorSesion(String sesionId);

    /** Bandeja del personal: consultas escaladas, las mas antiguas primero. */
    Pagina<Consulta> listarEscaladas(CriterioPaginacion criterio);

    /** Abiertas (aun a cargo del bot) sin actividad desde antes del limite: abandonadas. */
    List<Consulta> abiertasSinActividadAntesDe(Instant limite);

    /** Escaladas que el personal no atendio antes del limite. */
    List<Consulta> escaladasSinActividadAntesDe(Instant limite);

    Pagina<Consulta> listar(PeriodoMedicion periodo, CanalConsulta canal, ResultadoConsulta resultado,
                            CriterioPaginacion criterio);
}

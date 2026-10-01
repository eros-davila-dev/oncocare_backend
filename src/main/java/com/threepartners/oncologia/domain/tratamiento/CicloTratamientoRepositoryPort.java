package com.threepartners.oncologia.domain.tratamiento;

import com.threepartners.oncologia.domain.shared.CriterioPaginacion;
import com.threepartners.oncologia.domain.shared.Pagina;

import java.util.List;
import java.util.Optional;

public interface CicloTratamientoRepositoryPort {

    CicloTratamiento guardar(CicloTratamiento ciclo);

    Optional<CicloTratamiento> buscarPorId(Long id);

    List<CicloTratamiento> listarPorPaciente(Long pacienteId);

    Pagina<CicloTratamiento> listar(Long pacienteId, TipoTratamiento tipo, CriterioPaginacion criterio);

    /** Promedio de avance de los esquemas (sesion actual / total), en porcentaje; null si no hay ciclos. */
    Double promedioCumplimiento();
}

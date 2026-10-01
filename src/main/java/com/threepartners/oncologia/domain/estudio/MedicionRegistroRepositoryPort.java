package com.threepartners.oncologia.domain.estudio;

import com.threepartners.oncologia.domain.shared.CriterioPaginacion;
import com.threepartners.oncologia.domain.shared.Pagina;

import java.time.Instant;
import java.util.Optional;

public interface MedicionRegistroRepositoryPort {

    MedicionRegistro guardar(MedicionRegistro medicion);

    Optional<MedicionRegistro> buscarPorId(Long id);

    /** Pasa a ABANDONADA toda sesion EN_CURSO iniciada antes del limite; devuelve cuantas. */
    int marcarAbandonadasIniciadasAntesDe(Instant limite);

    /** Evita importar dos veces la misma fila de la ficha de tiempos del pretest. */
    boolean existeManual(Long pacienteId, TipoMedicion tipo, Instant inicio);

    Pagina<MedicionRegistro> listar(PeriodoMedicion periodo, TipoMedicion tipo, CanalMedicion canal,
                                    EstadoMedicion estado, CriterioPaginacion criterio);
}

package com.threepartners.oncologia.domain.estudio;

import java.util.List;

/**
 * Las mismas reglas de {@link IndicadoresEstudioRepositoryPort}, pero fila
 * por fila: lo exportado debe sumar exactamente lo que muestra el indicador.
 */
public interface RegistrosEstudioRepositoryPort {

    List<RegistrosFichas.Tiempo> tiempos(FiltroIndicadores filtro);

    List<RegistrosFichas.Asistencia> asistencias(FiltroIndicadores filtro);

    List<RegistrosFichas.ConsultaCerrada> consultas(FiltroIndicadores filtro);
}

package com.threepartners.oncologia.domain.estudio;

import java.util.Map;

/**
 * Conteos agregados en la base de datos. Nunca devuelve porcentajes ni
 * promedios: las formulas viven en {@link CalculoIndicadores}.
 */
public interface IndicadoresEstudioRepositoryPort {

    ConteosIndicadores contar(FiltroIndicadores filtro);

    /**
     * Conteos por participante incluido (clave: codigo Pnn), para la tabla
     * pareada de la prueba de Wilcoxon. Ignora el alcance del filtro: por
     * definicion es siempre la muestra.
     */
    Map<String, ConteosIndicadores> contarPorParticipante(FiltroIndicadores filtro);
}

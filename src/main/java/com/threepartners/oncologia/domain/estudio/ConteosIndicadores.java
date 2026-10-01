package com.threepartners.oncologia.domain.estudio;

/**
 * Lo unico que la persistencia aporta al calculo: conteos crudos. Las
 * formulas se aplican en {@link IndicadoresEstudio#de(ConteosIndicadores)}.
 */
public record ConteosIndicadores(
        long registros,
        long sumaSegundosRegistro,
        long inasistencias,
        long citasCumplidas,
        long consultasResueltas,
        long consultasResueltasBot,
        long consultasCerradas
) {

    public static ConteosIndicadores vacios() {
        return new ConteosIndicadores(0, 0, 0, 0, 0, 0, 0);
    }
}

package com.threepartners.oncologia.infrastructure.in.rest.dto.estudio;

import com.threepartners.oncologia.domain.estudio.IndicadoresEstudio;

/**
 * Los tres indicadores de la tesis con su n. Un valor null significa "sin
 * datos" (denominador cero), no cero.
 */
public record IndicadoresDto(
        Double tiempoPromedioRegistroMinutos,
        long registros,
        Double tasaAusentismo,
        long inasistencias,
        long citasCumplidas,
        long citasConDesenlace,
        Double nivelConsultasAtendidas,
        Double nivelConsultasAtendidasAutomatico,
        long consultasResueltas,
        long consultasResueltasBot,
        long consultasCerradas
) {

    public static IndicadoresDto de(IndicadoresEstudio i) {
        return new IndicadoresDto(i.tiempoPromedioRegistroMinutos(), i.registros(), i.tasaAusentismo(),
                i.inasistencias(), i.citasCumplidas(), i.citasConDesenlace(), i.nivelConsultasAtendidas(),
                i.nivelConsultasAtendidasAutomatico(), i.consultasResueltas(), i.consultasResueltasBot(),
                i.consultasCerradas());
    }
}

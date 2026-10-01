package com.threepartners.oncologia.domain.estudio;

/**
 * Valor de los tres indicadores de la tesis para un periodo y alcance, junto
 * con el n de cada uno (sin el n un porcentaje no se puede interpretar).
 *
 * ncaAutomatico es el aporte especifico del chatbot (solo RESUELTA_BOT); el
 * NCA de la hipotesis cuenta lo resuelto por el bot y por el personal, para
 * compararse de forma justa con el pretest, donde todo lo resolvia el
 * personal.
 */
public record IndicadoresEstudio(
        Double tiempoPromedioRegistroMinutos,
        long registros,
        Double tasaAusentismo,
        long inasistencias,
        long citasCumplidas,
        Double nivelConsultasAtendidas,
        Double nivelConsultasAtendidasAutomatico,
        long consultasResueltas,
        long consultasResueltasBot,
        long consultasCerradas
) {

    public static IndicadoresEstudio de(ConteosIndicadores c) {
        return new IndicadoresEstudio(
                CalculoIndicadores.tiempoPromedioRegistroMinutos(c.sumaSegundosRegistro(), c.registros()),
                c.registros(),
                CalculoIndicadores.tasaAusentismo(c.inasistencias(), c.citasCumplidas()),
                c.inasistencias(),
                c.citasCumplidas(),
                CalculoIndicadores.nivelConsultasAtendidas(c.consultasResueltas(), c.consultasCerradas()),
                CalculoIndicadores.nivelConsultasAtendidas(c.consultasResueltasBot(), c.consultasCerradas()),
                c.consultasResueltas(),
                c.consultasResueltasBot(),
                c.consultasCerradas());
    }

    public long citasConDesenlace() {
        return inasistencias + citasCumplidas;
    }
}

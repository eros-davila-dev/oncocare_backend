package com.threepartners.oncologia.domain.estudio;

/**
 * Desenlace de una consulta (indicador NCA). ESCALADA no es final: la
 * consulta espera al personal y todavia no entra al denominador.
 */
public enum ResultadoConsulta {
    RESUELTA_BOT,
    RESUELTA_PERSONAL,
    ESCALADA,
    NO_RESUELTA,
    ANULADA;

    public boolean esFinal() {
        return this == RESUELTA_BOT || this == RESUELTA_PERSONAL || this == NO_RESUELTA;
    }

    public boolean esResuelta() {
        return this == RESUELTA_BOT || this == RESUELTA_PERSONAL;
    }
}

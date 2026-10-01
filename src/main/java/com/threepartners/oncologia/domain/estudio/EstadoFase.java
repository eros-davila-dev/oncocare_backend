package com.threepartners.oncologia.domain.estudio;

public enum EstadoFase {
    ABIERTA,
    /** Fechas congeladas: los indicadores de la fase ya no pueden cambiar por una reconfiguracion. */
    CERRADA
}

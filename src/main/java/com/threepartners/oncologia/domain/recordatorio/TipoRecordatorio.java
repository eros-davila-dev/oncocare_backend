package com.threepartners.oncologia.domain.recordatorio;

import java.time.Duration;

/**
 * Momentos del recordatorio respecto de la hora de la cita. El olvido es la
 * causa principal de inasistencia (Alturbag, 44 % de los casos): recordar
 * con anticipacion y otra vez poco antes ataca directamente el TNS.
 */
public enum TipoRecordatorio {
    T72H(Duration.ofHours(72)),
    T24H(Duration.ofHours(24)),
    T2H(Duration.ofHours(2));

    private final Duration anticipacion;

    TipoRecordatorio(Duration anticipacion) {
        this.anticipacion = anticipacion;
    }

    public Duration anticipacion() {
        return anticipacion;
    }
}

package com.threepartners.oncologia.domain.estudio;

import com.threepartners.oncologia.domain.shared.ZonaHoraria;
import com.threepartners.oncologia.domain.shared.exception.ValidacionDeNegocioException;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Rango de fechas inclusivo [desde, hasta] interpretado en hora de Lima. Para
 * filtrar timestamps se usa el intervalo semiabierto [inicio, finExclusivo),
 * que no pierde ni duplica eventos justo a medianoche.
 */
public record PeriodoMedicion(LocalDate desde, LocalDate hasta) {

    public PeriodoMedicion {
        if (desde == null || hasta == null) {
            throw new ValidacionDeNegocioException("El periodo de medicion requiere fecha de inicio y de fin");
        }
        if (hasta.isBefore(desde)) {
            throw new ValidacionDeNegocioException("La fecha de fin del periodo no puede ser anterior a la de inicio");
        }
    }

    public Instant inicio() {
        return desde.atStartOfDay(ZonaHoraria.LIMA).toInstant();
    }

    public Instant finExclusivo() {
        return hasta.plusDays(1).atStartOfDay(ZonaHoraria.LIMA).toInstant();
    }

    public boolean contiene(LocalDate fecha) {
        return fecha != null && !fecha.isBefore(desde) && !fecha.isAfter(hasta);
    }
}

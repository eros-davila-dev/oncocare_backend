package com.threepartners.oncologia.domain.estudio;

import com.threepartners.oncologia.domain.shared.exception.ValidacionDeNegocioException;

import java.util.EnumSet;
import java.util.Set;

/**
 * Parametros de un calculo de indicadores. Los valores por defecto
 * corresponden a la operacionalizacion de la hipotesis (TPR sobre registro de
 * citas por el personal: canales INTRANET y MANUAL).
 */
public record FiltroIndicadores(
        PeriodoMedicion periodo,
        AlcanceIndicador alcance,
        TipoMedicion tipoRegistro,
        Set<CanalMedicion> canalesRegistro
) {

    public FiltroIndicadores {
        if (periodo == null) {
            throw new ValidacionDeNegocioException("El calculo de indicadores requiere un periodo");
        }
        alcance = alcance != null ? alcance : AlcanceIndicador.MUESTRA;
        tipoRegistro = tipoRegistro != null ? tipoRegistro : TipoMedicion.REGISTRO_CITA;
        canalesRegistro = canalesRegistro == null || canalesRegistro.isEmpty()
                ? CanalMedicion.porDefectoParaHipotesis()
                : EnumSet.copyOf(canalesRegistro);
    }

    public static FiltroIndicadores deHipotesis(PeriodoMedicion periodo, AlcanceIndicador alcance) {
        return new FiltroIndicadores(periodo, alcance, null, null);
    }
}

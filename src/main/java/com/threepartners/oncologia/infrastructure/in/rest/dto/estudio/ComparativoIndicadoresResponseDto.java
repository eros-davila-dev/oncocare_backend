package com.threepartners.oncologia.infrastructure.in.rest.dto.estudio;

import com.threepartners.oncologia.domain.estudio.AlcanceIndicador;
import com.threepartners.oncologia.domain.estudio.ComparativoIndicadores;
import com.threepartners.oncologia.domain.estudio.IndicadoresEstudio;
import com.threepartners.oncologia.domain.estudio.PeriodoMedicion;

import java.time.LocalDate;

public record ComparativoIndicadoresResponseDto(
        AlcanceIndicador alcance,
        FaseIndicadoresDto pretest,
        FaseIndicadoresDto postest,
        VariacionDto tiempoPromedioRegistro,
        VariacionDto tasaAusentismo,
        VariacionDto nivelConsultasAtendidas
) {

    public static ComparativoIndicadoresResponseDto de(ComparativoIndicadores c) {
        return new ComparativoIndicadoresResponseDto(c.alcance(),
                FaseIndicadoresDto.de(c.periodoPretest(), c.pretest()),
                FaseIndicadoresDto.de(c.periodoPostest(), c.postest()),
                VariacionDto.de(c.tiempoPromedioRegistro()),
                VariacionDto.de(c.tasaAusentismo()),
                VariacionDto.de(c.nivelConsultasAtendidas()));
    }

    public record FaseIndicadoresDto(LocalDate desde, LocalDate hasta, IndicadoresDto indicadores) {

        static FaseIndicadoresDto de(PeriodoMedicion periodo, IndicadoresEstudio i) {
            return new FaseIndicadoresDto(periodo.desde(), periodo.hasta(), IndicadoresDto.de(i));
        }
    }

    public record VariacionDto(Double diferencia, Double porcentaje) {

        static VariacionDto de(ComparativoIndicadores.Variacion v) {
            return new VariacionDto(v.diferencia(), v.porcentaje());
        }
    }
}

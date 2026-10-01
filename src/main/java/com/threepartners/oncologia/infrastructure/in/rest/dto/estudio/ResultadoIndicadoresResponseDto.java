package com.threepartners.oncologia.infrastructure.in.rest.dto.estudio;

import com.threepartners.oncologia.application.estudio.ConsultarIndicadoresEstudioUseCase.ResultadoIndicadores;
import com.threepartners.oncologia.domain.estudio.AlcanceIndicador;
import com.threepartners.oncologia.domain.estudio.CanalMedicion;
import com.threepartners.oncologia.domain.estudio.Fase;
import com.threepartners.oncologia.domain.estudio.TipoMedicion;

import java.time.LocalDate;
import java.util.Set;

public record ResultadoIndicadoresResponseDto(
        Fase fase,
        LocalDate desde,
        LocalDate hasta,
        AlcanceIndicador alcance,
        TipoMedicion tipoRegistro,
        Set<CanalMedicion> canalesRegistro,
        IndicadoresDto indicadores
) {

    public static ResultadoIndicadoresResponseDto de(ResultadoIndicadores r) {
        var f = r.filtro();
        return new ResultadoIndicadoresResponseDto(r.fase(), f.periodo().desde(), f.periodo().hasta(), f.alcance(),
                f.tipoRegistro(), f.canalesRegistro(), IndicadoresDto.de(r.indicadores()));
    }
}

package com.threepartners.oncologia.infrastructure.in.rest.dto.estudio;

import com.threepartners.oncologia.domain.estudio.FilaPareada;

public record FilaPareadaResponseDto(String codigo, IndicadoresDto pretest, IndicadoresDto postest) {

    public static FilaPareadaResponseDto de(FilaPareada f) {
        return new FilaPareadaResponseDto(f.codigo(), IndicadoresDto.de(f.pretest()), IndicadoresDto.de(f.postest()));
    }
}

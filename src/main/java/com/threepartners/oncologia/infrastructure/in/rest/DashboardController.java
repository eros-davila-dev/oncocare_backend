package com.threepartners.oncologia.infrastructure.in.rest;

import com.threepartners.oncologia.application.dashboard.ConsultarIndicadoresUseCase;
import com.threepartners.oncologia.infrastructure.in.rest.dto.dashboard.IndicadoresResponseDto;
import com.threepartners.oncologia.infrastructure.in.rest.dto.estudio.IndicadoresDto;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final ConsultarIndicadoresUseCase consultarIndicadoresUseCase;

    @GetMapping("/indicadores")
    public IndicadoresResponseDto indicadores(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {

        var resumen = consultarIndicadoresUseCase.ejecutar(desde, hasta);
        return new IndicadoresResponseDto(
                resumen.periodo().desde(),
                resumen.periodo().hasta(),
                IndicadoresDto.de(resumen.indicadores()),
                resumen.cumplimientoTratamientoPorcentaje());
    }
}

package com.threepartners.oncologia.infrastructure.in.rest;

import com.threepartners.oncologia.application.estudio.ConsultarIndicadoresEstudioUseCase;
import com.threepartners.oncologia.domain.estudio.AlcanceIndicador;
import com.threepartners.oncologia.domain.estudio.CanalMedicion;
import com.threepartners.oncologia.domain.estudio.Fase;
import com.threepartners.oncologia.domain.estudio.TipoMedicion;
import com.threepartners.oncologia.infrastructure.in.rest.dto.estudio.AnalisisPareadoResponseDto;
import com.threepartners.oncologia.infrastructure.in.rest.dto.estudio.ComparativoIndicadoresResponseDto;
import com.threepartners.oncologia.infrastructure.in.rest.dto.estudio.FilaPareadaResponseDto;
import com.threepartners.oncologia.infrastructure.in.rest.dto.estudio.ResultadoIndicadoresResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Indicadores de la tesis: TPR (min), TNS (%) y NCA (%).
 */
@RestController
@RequestMapping("/api/v1/estudio/indicadores")
@RequiredArgsConstructor
public class EstudioIndicadoresController {

    private final ConsultarIndicadoresEstudioUseCase consultarIndicadoresUseCase;

    @Operation(summary = "Indicadores de una fase o de un rango de fechas",
            description = "Con fase usa las fechas configuradas de la fase; si no, desde/hasta (por defecto los ultimos 30 dias). "
                    + "alcance=MUESTRA (por defecto) considera solo a los participantes incluidos.")
    @GetMapping
    public ResultadoIndicadoresResponseDto calcular(
            @RequestParam(required = false) Fase fase,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) AlcanceIndicador alcance,
            @RequestParam(required = false) TipoMedicion tipoRegistro,
            @RequestParam(required = false) Set<CanalMedicion> canales) {
        return ResultadoIndicadoresResponseDto.de(
                consultarIndicadoresUseCase.calcular(fase, desde, hasta, alcance, tipoRegistro, canales));
    }

    @GetMapping("/comparativo")
    public ComparativoIndicadoresResponseDto comparativo(
            @RequestParam(required = false) AlcanceIndicador alcance,
            @RequestParam(required = false) TipoMedicion tipoRegistro,
            @RequestParam(required = false) Set<CanalMedicion> canales) {
        return ComparativoIndicadoresResponseDto.de(
                consultarIndicadoresUseCase.comparativo(alcance, tipoRegistro, canales));
    }

    @Operation(summary = "Tabla pareada por participante (entrada de la prueba de Wilcoxon)")
    @GetMapping("/pareado")
    public List<FilaPareadaResponseDto> pareado(
            @RequestParam(required = false) TipoMedicion tipoRegistro,
            @RequestParam(required = false) Set<CanalMedicion> canales) {
        return consultarIndicadoresUseCase.pareado(tipoRegistro, canales).stream()
                .map(FilaPareadaResponseDto::de)
                .toList();
    }

    @Operation(summary = "Prueba de Wilcoxon preliminar por indicador (convenciones de SPSS)",
            description = "Vista previa sobre la tabla pareada; el analisis oficial se hace en SPSS con la exportacion.")
    @GetMapping("/wilcoxon")
    public AnalisisPareadoResponseDto wilcoxon(
            @RequestParam(required = false) TipoMedicion tipoRegistro,
            @RequestParam(required = false) Set<CanalMedicion> canales) {
        return AnalisisPareadoResponseDto.de(consultarIndicadoresUseCase.analisis(tipoRegistro, canales));
    }
}

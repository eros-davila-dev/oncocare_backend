package com.threepartners.oncologia.infrastructure.in.rest;

import com.threepartners.oncologia.application.dashboard.ConsultarActividadUseCase;
import com.threepartners.oncologia.application.dashboard.ConsultarIndicadoresUseCase;
import com.threepartners.oncologia.domain.dashboard.ActividadPeriodo;
import com.threepartners.oncologia.domain.shared.ZonaHoraria;
import com.threepartners.oncologia.infrastructure.in.rest.archivo.GeneradorExportacionActividad;
import com.threepartners.oncologia.infrastructure.in.rest.dto.dashboard.IndicadoresResponseDto;
import com.threepartners.oncologia.infrastructure.in.rest.dto.estudio.IndicadoresDto;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private static final MediaType XLSX =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    private static final DateTimeFormatter SELLO = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm");

    private final ConsultarIndicadoresUseCase consultarIndicadoresUseCase;
    private final ConsultarActividadUseCase consultarActividadUseCase;
    private final GeneradorExportacionActividad generadorExportacionActividad;

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

    /** Registros, citas, recordatorios y consultas del periodo: totales y una fila por dia. */
    @GetMapping("/actividad")
    public ActividadPeriodo.Resumen actividad(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return consultarActividadUseCase.resumen(desde, hasta);
    }

    /** Fila por fila (con nombres de pacientes). */
    @GetMapping("/actividad/detalle")
    public ActividadPeriodo.Filas actividadDetalle(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return consultarActividadUseCase.detalle(desde, hasta);
    }

    @GetMapping("/actividad/exportar.xlsx")
    public ResponseEntity<byte[]> exportarActividad(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            HttpServletRequest request) {
        var datos = consultarActividadUseCase.exportar(desde, hasta, AutenticacionActual.usuarioId(),
                AutenticacionActual.ipOrigen(request));
        String nombre = "actividad-" + datos.resumen().desde() + "-a-" + datos.resumen().hasta() + "-"
                + datos.generadoEn().atZone(ZonaHoraria.LIMA).format(SELLO) + ".xlsx";
        return ResponseEntity.ok()
                .contentType(XLSX)
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(nombre).build().toString())
                .body(generadorExportacionActividad.generar(datos));
    }
}

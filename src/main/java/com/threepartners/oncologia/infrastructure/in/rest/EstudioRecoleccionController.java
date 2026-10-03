package com.threepartners.oncologia.infrastructure.in.rest;

import com.threepartners.oncologia.application.estudio.RecoleccionPorSesionUseCase;
import com.threepartners.oncologia.domain.estudio.Fase;
import com.threepartners.oncologia.domain.estudio.RecoleccionSesiones;
import com.threepartners.oncologia.domain.shared.ZonaHoraria;
import com.threepartners.oncologia.infrastructure.in.rest.archivo.GeneradorExportacionRecoleccion;
import io.swagger.v3.oas.annotations.Operation;
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
import java.util.Locale;

/**
 * Recoleccion de datos por sesion (tesis v8): resumen por sesion, fichas
 * evento por evento y descarga en Excel con las columnas del Instrumento.
 * Las filas no llevan datos personales: solo el codigo anonimo PAC-NNNN.
 */
@RestController
@RequestMapping("/api/v1/estudio/recoleccion")
@RequiredArgsConstructor
public class EstudioRecoleccionController {

    private static final MediaType XLSX =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    private static final DateTimeFormatter SELLO = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm");

    private final RecoleccionPorSesionUseCase useCase;
    private final GeneradorExportacionRecoleccion generador;

    @Operation(summary = "TPR, ausentismo y NCA de cada sesion (lunes, miercoles y viernes) de una fase")
    @GetMapping
    public RecoleccionSesiones.Resumen resumen(@RequestParam Fase fase) {
        return useCase.resumen(fase);
    }

    @Operation(summary = "Fichas evento por evento de una sesion, o de todas las sesiones si no se indica fecha")
    @GetMapping("/detalle")
    public RecoleccionSesiones.Detalle detalle(@RequestParam Fase fase,
                                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return useCase.detalle(fase, fecha);
    }

    @Operation(summary = "Excel con las hojas del Instrumento: TPR, Ausentismo, Consultas, Sesiones y SPSS_Independientes")
    @GetMapping("/exportar.xlsx")
    public ResponseEntity<byte[]> exportar(@RequestParam Fase fase, HttpServletRequest request) {
        var datos = useCase.exportar(fase, AutenticacionActual.usuarioId(), AutenticacionActual.ipOrigen(request));
        String nombre = "recoleccion-" + fase.name().toLowerCase(Locale.ROOT) + "-"
                + datos.generadoEn().atZone(ZonaHoraria.LIMA).format(SELLO) + ".xlsx";
        return ResponseEntity.ok()
                .contentType(XLSX)
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(nombre).build().toString())
                .body(generador.generar(datos));
    }
}

package com.threepartners.oncologia.infrastructure.in.rest;

import com.threepartners.oncologia.application.estudio.ExportarDatosEstudioUseCase;
import com.threepartners.oncologia.domain.estudio.AlcanceIndicador;
import com.threepartners.oncologia.domain.estudio.CanalMedicion;
import com.threepartners.oncologia.domain.estudio.Fase;
import com.threepartners.oncologia.domain.estudio.TipoMedicion;
import com.threepartners.oncologia.domain.shared.ZonaHoraria;
import com.threepartners.oncologia.infrastructure.in.rest.archivo.GeneradorExportacionFichas;
import com.threepartners.oncologia.infrastructure.in.rest.archivo.GeneradorExportacionSpss;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;

/**
 * Descargas del estudio en .xlsx: la tabla para SPSS y las fichas del Anexo 2.
 * Sin cache: son datos de salud y cambian mientras la fase esta abierta.
 */
@RestController
@RequestMapping("/api/v1/estudio/exportaciones")
@RequiredArgsConstructor
public class EstudioExportacionesController {

    private static final MediaType XLSX =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    private static final DateTimeFormatter SELLO = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm");

    private final ExportarDatosEstudioUseCase exportarDatosUseCase;
    private final GeneradorExportacionSpss generadorSpss;
    private final GeneradorExportacionFichas generadorFichas;

    @Operation(summary = "Libro para SPSS: tabla pareada, detalle, resumen con Wilcoxon, diccionario y metadatos")
    @GetMapping("/spss.xlsx")
    public ResponseEntity<byte[]> spss(@RequestParam(required = false) TipoMedicion tipoRegistro,
                                       @RequestParam(required = false) Set<CanalMedicion> canales,
                                       HttpServletRequest request) {
        var datos = exportarDatosUseCase.spss(tipoRegistro, canales,
                AutenticacionActual.usuarioId(), AutenticacionActual.ipOrigen(request));
        return descarga("estudio-spss-" + sello(datos.generadoEn()) + ".xlsx", generadorSpss.generar(datos));
    }

    @Operation(summary = "Las tres fichas del Anexo 2 de una fase, con los indicadores recalculables en Excel")
    @GetMapping("/fichas.xlsx")
    public ResponseEntity<byte[]> fichas(@RequestParam Fase fase,
                                         @RequestParam(required = false) AlcanceIndicador alcance,
                                         @RequestParam(required = false) TipoMedicion tipoRegistro,
                                         @RequestParam(required = false) Set<CanalMedicion> canales,
                                         HttpServletRequest request) {
        var datos = exportarDatosUseCase.fichas(fase, alcance, tipoRegistro, canales,
                AutenticacionActual.usuarioId(), AutenticacionActual.ipOrigen(request));
        String nombre = "fichas-" + fase.name().toLowerCase(Locale.ROOT) + "-" + sello(datos.generadoEn()) + ".xlsx";
        return descarga(nombre, generadorFichas.generar(datos));
    }

    private static ResponseEntity<byte[]> descarga(String nombre, byte[] contenido) {
        return ResponseEntity.ok()
                .contentType(XLSX)
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(nombre).build().toString())
                .body(contenido);
    }

    private static String sello(Instant instante) {
        return instante.atZone(ZonaHoraria.LIMA).format(SELLO);
    }
}

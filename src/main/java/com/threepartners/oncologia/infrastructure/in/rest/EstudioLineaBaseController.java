package com.threepartners.oncologia.infrastructure.in.rest;

import com.threepartners.oncologia.application.estudio.LineaBaseUseCase;
import com.threepartners.oncologia.domain.estudio.Fase;
import com.threepartners.oncologia.domain.estudio.LineaBaseSesion;
import com.threepartners.oncologia.infrastructure.in.rest.archivo.LectorHojaCalculo;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Linea base del pretest por sesion (valores de la hoja SPSS_Independientes
 * del instrumento manual validado por la fundacion).
 */
@RestController
@RequestMapping("/api/v1/estudio/linea-base")
@RequiredArgsConstructor
public class EstudioLineaBaseController {

    private static final String HOJA_INSTRUMENTO = "SPSS_Independientes";

    private final LineaBaseUseCase lineaBaseUseCase;
    private final LectorHojaCalculo lectorHojaCalculo;

    @Operation(summary = "Sesiones cargadas desde el instrumento: pretest (linea base) o postest importado")
    @GetMapping
    public List<LineaBaseSesion> listar(@RequestParam(defaultValue = "PRETEST") Fase fase) {
        return lineaBaseUseCase.listar(fase);
    }

    @Operation(summary = "Importa el postest medido en el servidor de la fundacion, rotulado con su origen",
            description = "Lee la hoja SPSS_Independientes; solo guarda las filas de postest. No crea pacientes ni eventos.")
    @PostMapping(value = "/postest-importado", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public LineaBaseUseCase.Resultado importarPostest(@RequestPart("archivo") MultipartFile archivo, HttpServletRequest request) {
        return lineaBaseUseCase.importarPostest(lectorHojaCalculo.leer(archivo, HOJA_INSTRUMENTO),
                AutenticacionActual.usuarioId(), AutenticacionActual.ipOrigen(request));
    }

    @Operation(summary = "Carga la linea base del pretest desde el instrumento (.xlsx o .csv)",
            description = "Lee la hoja SPSS_Independientes; solo guarda las filas de pretest. Las de postest se ignoran.")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public LineaBaseUseCase.Resultado cargar(@RequestPart("archivo") MultipartFile archivo, HttpServletRequest request) {
        return lineaBaseUseCase.cargar(lectorHojaCalculo.leer(archivo, HOJA_INSTRUMENTO),
                AutenticacionActual.usuarioId(), AutenticacionActual.ipOrigen(request));
    }
}

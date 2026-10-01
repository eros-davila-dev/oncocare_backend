package com.threepartners.oncologia.infrastructure.in.rest;

import com.threepartners.oncologia.application.estudio.CapturarFichaEstudioUseCase;
import com.threepartners.oncologia.application.estudio.FichasEstudio.FichaAsistencia;
import com.threepartners.oncologia.application.estudio.FichasEstudio.FichaConsulta;
import com.threepartners.oncologia.application.estudio.FichasEstudio.FichaTiempo;
import com.threepartners.oncologia.application.estudio.FichasEstudio.ResultadoImportacion;
import com.threepartners.oncologia.application.estudio.FichasEstudio.TipoFicha;
import com.threepartners.oncologia.application.estudio.ImportarFichaEstudioUseCase;
import com.threepartners.oncologia.domain.shared.exception.ValidacionDeNegocioException;
import com.threepartners.oncologia.infrastructure.in.rest.archivo.GeneradorPlantillaFicha;
import com.threepartners.oncologia.infrastructure.in.rest.archivo.LectorHojaCalculo;
import com.threepartners.oncologia.infrastructure.in.rest.dto.cita.CitaResponseDto;
import com.threepartners.oncologia.infrastructure.in.rest.dto.estudio.ConsultaResponseDto;
import com.threepartners.oncologia.infrastructure.in.rest.dto.estudio.FichaAsistenciaRequestDto;
import com.threepartners.oncologia.infrastructure.in.rest.dto.estudio.FichaConsultaRequestDto;
import com.threepartners.oncologia.infrastructure.in.rest.dto.estudio.FichaTiempoRequestDto;
import com.threepartners.oncologia.infrastructure.in.rest.dto.estudio.MedicionRegistroResponseDto;
import com.threepartners.oncologia.infrastructure.in.rest.mapper.CitaRestMapper;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;

/**
 * Las tres fichas de recoleccion del Anexo 2 de la tesis: captura individual,
 * importacion desde hoja de calculo y plantilla descargable.
 */
@RestController
@RequestMapping("/api/v1/estudio/fichas")
@RequiredArgsConstructor
public class EstudioFichasController {

    private static final MediaType XLSX =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final CapturarFichaEstudioUseCase capturarFichaUseCase;
    private final ImportarFichaEstudioUseCase importarFichaUseCase;
    private final LectorHojaCalculo lectorHojaCalculo;
    private final GeneradorPlantillaFicha generadorPlantilla;
    private final CitaRestMapper citaRestMapper;

    @PostMapping("/tiempos")
    public ResponseEntity<MedicionRegistroResponseDto> tiempo(@Valid @RequestBody FichaTiempoRequestDto dto,
                                                              HttpServletRequest request) {
        var medicion = capturarFichaUseCase.registrarTiempo(
                new FichaTiempo(dto.pacienteId(), dto.tipo(), dto.fecha(), dto.horaInicio(), dto.horaFin(), dto.observacion()),
                AutenticacionActual.usuarioId(), AutenticacionActual.ipOrigen(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(MedicionRegistroResponseDto.de(medicion));
    }

    @PostMapping("/asistencias")
    public ResponseEntity<CitaResponseDto> asistencia(@Valid @RequestBody FichaAsistenciaRequestDto dto,
                                                      HttpServletRequest request) {
        var cita = capturarFichaUseCase.registrarAsistencia(
                new FichaAsistencia(dto.pacienteId(), dto.fecha(), dto.hora(), dto.tipoConsulta(), dto.asistio()),
                AutenticacionActual.usuarioId(), AutenticacionActual.ipOrigen(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(citaRestMapper.aResponse(cita));
    }

    @PostMapping("/consultas")
    public ResponseEntity<ConsultaResponseDto> consulta(@Valid @RequestBody FichaConsultaRequestDto dto,
                                                        HttpServletRequest request) {
        var consulta = capturarFichaUseCase.registrarConsulta(
                new FichaConsulta(dto.fecha(), dto.hora(), dto.canal(), dto.pacienteId(), dto.resumen(),
                        dto.resuelta(), dto.observacion()),
                AutenticacionActual.usuarioId(), AutenticacionActual.ipOrigen(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(ConsultaResponseDto.de(consulta));
    }

    @Operation(summary = "Importa una ficha desde .xlsx o .csv",
            description = "Con aplicar=false solo valida y devuelve los errores por fila (vista previa). "
                    + "Con aplicar=true guarda todas las filas solo si ninguna tiene errores.")
    @PostMapping(value = "/{ficha}/importar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResultadoImportacion importar(@PathVariable String ficha,
                                         @RequestPart("archivo") MultipartFile archivo,
                                         @RequestParam(defaultValue = "false") boolean aplicar,
                                         HttpServletRequest request) {
        return importarFichaUseCase.ejecutar(tipoFicha(ficha), lectorHojaCalculo.leer(archivo), aplicar,
                AutenticacionActual.usuarioId(), AutenticacionActual.ipOrigen(request));
    }

    @GetMapping("/{ficha}/plantilla")
    public ResponseEntity<byte[]> plantilla(@PathVariable String ficha) {
        TipoFicha tipo = tipoFicha(ficha);
        return ResponseEntity.ok()
                .contentType(XLSX)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("plantilla-ficha-" + tipo.name().toLowerCase(Locale.ROOT) + ".xlsx")
                        .build().toString())
                .body(generadorPlantilla.generar(tipo));
    }

    private static TipoFicha tipoFicha(String valor) {
        try {
            return TipoFicha.valueOf(valor.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ValidacionDeNegocioException("Ficha desconocida '" + valor + "' (use tiempos, asistencias o consultas)");
        }
    }
}

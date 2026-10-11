package com.threepartners.oncologia.infrastructure.in.rest;

import com.threepartners.oncologia.application.estudio.EventosImportadosUseCase;
import com.threepartners.oncologia.domain.estudio.EventoImportado;
import com.threepartners.oncologia.domain.estudio.Fase;
import com.threepartners.oncologia.domain.estudio.TipoEventoImportado;
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Eventos por paciente del instrumento (codigo anonimo, sin crear pacientes):
 * pretest de las fichas manuales y postest exportado del servidor.
 */
@RestController
@RequestMapping("/api/v1/estudio/eventos-importados")
@RequiredArgsConstructor
public class EstudioEventosImportadosController {

    private final EventosImportadosUseCase useCase;
    private final LectorHojaCalculo lectorHojaCalculo;

    @Operation(summary = "Eventos importados de un tipo y una etapa")
    @GetMapping
    public List<EventoImportado> listar(@RequestParam TipoEventoImportado tipo, @RequestParam Fase fase) {
        return useCase.listar(tipo, fase);
    }

    @Operation(summary = "Importa las hojas TPR_, Ausentismo_ y Consultas_ (PRE y POST) del instrumento")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public EventosImportadosUseCase.Resultado cargar(@RequestPart("archivo") MultipartFile archivo, HttpServletRequest request) {
        Map<String, List<Map<String, String>>> hojas = new LinkedHashMap<>();
        for (String hoja : EventosImportadosUseCase.HOJAS.keySet()) {
            lectorHojaCalculo.leerHojaSiExiste(archivo, hoja).ifPresent(filas -> hojas.put(hoja, filas));
        }
        return useCase.cargar(hojas, AutenticacionActual.usuarioId(), AutenticacionActual.ipOrigen(request));
    }
}

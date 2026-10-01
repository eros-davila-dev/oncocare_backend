package com.threepartners.oncologia.infrastructure.in.rest;

import com.threepartners.oncologia.application.tratamiento.ActualizarEstadoCicloTratamientoUseCase;
import com.threepartners.oncologia.application.tratamiento.ConsultarCicloTratamientoUseCase;
import com.threepartners.oncologia.application.tratamiento.ProgramarCicloTratamientoUseCase;
import com.threepartners.oncologia.domain.shared.CriterioPaginacion;
import com.threepartners.oncologia.domain.tratamiento.TipoTratamiento;
import com.threepartners.oncologia.infrastructure.in.rest.dto.PaginaResponseDto;
import com.threepartners.oncologia.infrastructure.in.rest.dto.tratamiento.ActualizarEstadoCicloRequestDto;
import com.threepartners.oncologia.infrastructure.in.rest.dto.tratamiento.CicloTratamientoRequestDto;
import com.threepartners.oncologia.infrastructure.in.rest.dto.tratamiento.CicloTratamientoResponseDto;
import com.threepartners.oncologia.infrastructure.in.rest.mapper.CicloTratamientoRestMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tratamientos")
@RequiredArgsConstructor
public class TratamientoController {

    private final ProgramarCicloTratamientoUseCase programarCicloTratamientoUseCase;
    private final ActualizarEstadoCicloTratamientoUseCase actualizarEstadoCicloTratamientoUseCase;
    private final ConsultarCicloTratamientoUseCase consultarCicloTratamientoUseCase;
    private final CicloTratamientoRestMapper mapper;

    @PostMapping
    public ResponseEntity<CicloTratamientoResponseDto> programar(@Valid @RequestBody CicloTratamientoRequestDto dto, HttpServletRequest request) {
        var ciclo = programarCicloTratamientoUseCase.ejecutar(
                mapper.aDominio(dto), AutenticacionActual.usuarioId(), AutenticacionActual.ipOrigen(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.aResponse(ciclo));
    }

    @PatchMapping("/{id}/estado")
    public CicloTratamientoResponseDto actualizarEstado(@PathVariable Long id, @Valid @RequestBody ActualizarEstadoCicloRequestDto dto) {
        var ciclo = actualizarEstadoCicloTratamientoUseCase.ejecutar(id, dto.estado(), dto.observaciones());
        return mapper.aResponse(ciclo);
    }

    @GetMapping("/{id}")
    public CicloTratamientoResponseDto porId(@PathVariable Long id) {
        return mapper.aResponse(consultarCicloTratamientoUseCase.porId(id));
    }

    @GetMapping("/paciente/{pacienteId}")
    public List<CicloTratamientoResponseDto> porPaciente(@PathVariable Long pacienteId) {
        return consultarCicloTratamientoUseCase.porPaciente(pacienteId).stream().map(mapper::aResponse).toList();
    }

    @GetMapping
    public PaginaResponseDto<CicloTratamientoResponseDto> listar(
            @RequestParam(required = false) Long pacienteId,
            @RequestParam(required = false) TipoTratamiento tipo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        var pagina = consultarCicloTratamientoUseCase.listar(pacienteId, tipo, CriterioPaginacion.de(page, size));
        return PaginaResponseDto.de(pagina, mapper::aResponse);
    }
}

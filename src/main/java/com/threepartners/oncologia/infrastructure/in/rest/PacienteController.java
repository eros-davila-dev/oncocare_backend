package com.threepartners.oncologia.infrastructure.in.rest;

import com.threepartners.oncologia.application.paciente.ActualizarPacienteUseCase;
import com.threepartners.oncologia.application.paciente.CompletarPerfilPacienteUseCase;
import com.threepartners.oncologia.application.paciente.ConsultarEstadisticasPacientesUseCase;
import com.threepartners.oncologia.application.paciente.ConsultarPacienteUseCase;
import com.threepartners.oncologia.application.paciente.RegistrarPacienteUseCase;
import com.threepartners.oncologia.domain.paciente.EstadoTratamientoPaciente;
import com.threepartners.oncologia.domain.shared.CriterioPaginacion;
import com.threepartners.oncologia.infrastructure.in.rest.dto.PaginaResponseDto;
import com.threepartners.oncologia.infrastructure.in.rest.dto.paciente.EstadisticasPacientesResponseDto;
import com.threepartners.oncologia.infrastructure.in.rest.dto.paciente.PacienteRequestDto;
import com.threepartners.oncologia.infrastructure.in.rest.dto.paciente.PacienteResponseDto;
import com.threepartners.oncologia.infrastructure.in.rest.dto.paciente.PacienteResumenResponseDto;
import com.threepartners.oncologia.infrastructure.in.rest.mapper.PacienteRestMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pacientes")
@RequiredArgsConstructor
public class PacienteController {

    private final RegistrarPacienteUseCase registrarPacienteUseCase;
    private final ActualizarPacienteUseCase actualizarPacienteUseCase;
    private final CompletarPerfilPacienteUseCase completarPerfilPacienteUseCase;
    private final ConsultarPacienteUseCase consultarPacienteUseCase;
    private final ConsultarEstadisticasPacientesUseCase consultarEstadisticasPacientesUseCase;
    private final PacienteRestMapper mapper;

    /**
     * Paso 2 del autoservicio (seccion 7): el propio paciente autenticado
     * (cuenta ya verificada) completa su registro clinico.
     */
    @PostMapping("/mi-perfil")
    public ResponseEntity<PacienteResponseDto> completarMiPerfil(@Valid @RequestBody PacienteRequestDto dto, HttpServletRequest request) {
        var paciente = completarPerfilPacienteUseCase.ejecutar(
                AutenticacionActual.usuarioId(), mapper.aDominio(dto), AutenticacionActual.ipOrigen(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.aResponse(paciente));
    }

    @GetMapping("/mi-perfil")
    public PacienteResponseDto miPerfil() {
        return mapper.aResponse(consultarPacienteUseCase.miPerfil(AutenticacionActual.usuarioId()));
    }

    @PostMapping
    public ResponseEntity<PacienteResponseDto> registrar(@Valid @RequestBody PacienteRequestDto dto, HttpServletRequest request) {
        var paciente = registrarPacienteUseCase.ejecutar(
                mapper.aDominio(dto), AutenticacionActual.usuarioId(), AutenticacionActual.ipOrigen(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.aResponse(paciente));
    }

    @PutMapping("/{id}")
    public PacienteResponseDto actualizar(@PathVariable Long id, @Valid @RequestBody PacienteRequestDto dto, HttpServletRequest request) {
        var paciente = actualizarPacienteUseCase.ejecutar(
                id, mapper.aDominio(dto), AutenticacionActual.usuarioId(), AutenticacionActual.ipOrigen(request));
        return mapper.aResponse(paciente);
    }

    @GetMapping("/{id}")
    public PacienteResponseDto porId(@PathVariable Long id) {
        return mapper.aResponse(consultarPacienteUseCase.porId(id));
    }

    @GetMapping("/documento-disponible")
    public java.util.Map<String, Boolean> documentoDisponible(
            @RequestParam String documento,
            @RequestParam(required = false) Long idExcluido) {
        boolean existe = consultarPacienteUseCase.existeDocumento(documento, idExcluido);
        return java.util.Map.of("disponible", !existe);
    }

    @GetMapping
    public PaginaResponseDto<PacienteResponseDto> buscar(
            @RequestParam(required = false) String texto,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        var pagina = consultarPacienteUseCase.buscar(texto, CriterioPaginacion.de(page, size));
        return PaginaResponseDto.de(pagina, mapper::aResponse);
    }

    @GetMapping("/resumen")
    public PaginaResponseDto<PacienteResumenResponseDto> buscarResumen(
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) EstadoTratamientoPaciente estado,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        var pagina = consultarPacienteUseCase.buscarResumen(texto, estado, CriterioPaginacion.de(page, size));
        return PaginaResponseDto.de(pagina, mapper::aResumenResponse);
    }

    @GetMapping("/estadisticas")
    public EstadisticasPacientesResponseDto estadisticas() {
        return mapper.aEstadisticasResponse(consultarEstadisticasPacientesUseCase.ejecutar());
    }
}

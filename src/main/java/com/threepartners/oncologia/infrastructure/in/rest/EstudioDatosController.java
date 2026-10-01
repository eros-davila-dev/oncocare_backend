package com.threepartners.oncologia.infrastructure.in.rest;

import com.threepartners.oncologia.application.estudio.AnularDatoEstudioUseCase;
import com.threepartners.oncologia.application.estudio.ListarDatosEstudioUseCase;
import com.threepartners.oncologia.domain.estudio.CanalConsulta;
import com.threepartners.oncologia.domain.estudio.CanalMedicion;
import com.threepartners.oncologia.domain.estudio.EstadoMedicion;
import com.threepartners.oncologia.domain.estudio.Fase;
import com.threepartners.oncologia.domain.estudio.ResultadoConsulta;
import com.threepartners.oncologia.domain.estudio.TipoMedicion;
import com.threepartners.oncologia.domain.shared.CriterioPaginacion;
import com.threepartners.oncologia.infrastructure.in.rest.dto.PaginaResponseDto;
import com.threepartners.oncologia.infrastructure.in.rest.dto.estudio.AnularDatoRequestDto;
import com.threepartners.oncologia.infrastructure.in.rest.dto.estudio.ConsultaResponseDto;
import com.threepartners.oncologia.infrastructure.in.rest.dto.estudio.MedicionRegistroResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Revision de los datos crudos que alimentan los indicadores y su anulacion
 * justificada.
 */
@RestController
@RequestMapping("/api/v1/estudio")
@RequiredArgsConstructor
public class EstudioDatosController {

    private final ListarDatosEstudioUseCase listarDatosUseCase;
    private final AnularDatoEstudioUseCase anularDatoUseCase;

    @GetMapping("/mediciones")
    public PaginaResponseDto<MedicionRegistroResponseDto> mediciones(
            @RequestParam(required = false) Fase fase,
            @RequestParam(required = false) TipoMedicion tipo,
            @RequestParam(required = false) CanalMedicion canal,
            @RequestParam(required = false) EstadoMedicion estado,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var pagina = listarDatosUseCase.mediciones(fase, tipo, canal, estado, CriterioPaginacion.de(page, size));
        return PaginaResponseDto.de(pagina, MedicionRegistroResponseDto::de);
    }

    @GetMapping("/consultas")
    public PaginaResponseDto<ConsultaResponseDto> consultas(
            @RequestParam(required = false) Fase fase,
            @RequestParam(required = false) CanalConsulta canal,
            @RequestParam(required = false) ResultadoConsulta resultado,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var pagina = listarDatosUseCase.consultas(fase, canal, resultado, CriterioPaginacion.de(page, size));
        return PaginaResponseDto.de(pagina, ConsultaResponseDto::de);
    }

    @PatchMapping("/mediciones/{id}/anular")
    public MedicionRegistroResponseDto anularMedicion(@PathVariable Long id, @Valid @RequestBody AnularDatoRequestDto dto,
                                                      HttpServletRequest request) {
        return MedicionRegistroResponseDto.de(anularDatoUseCase.anularMedicion(id, dto.motivo(),
                AutenticacionActual.usuarioId(), AutenticacionActual.ipOrigen(request)));
    }

    @PatchMapping("/consultas/{id}/anular")
    public ConsultaResponseDto anularConsulta(@PathVariable Long id, @Valid @RequestBody AnularDatoRequestDto dto,
                                              HttpServletRequest request) {
        return ConsultaResponseDto.de(anularDatoUseCase.anularConsulta(id, dto.motivo(),
                AutenticacionActual.usuarioId(), AutenticacionActual.ipOrigen(request)));
    }
}

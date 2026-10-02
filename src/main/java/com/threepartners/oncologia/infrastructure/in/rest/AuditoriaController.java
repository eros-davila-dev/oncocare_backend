package com.threepartners.oncologia.infrastructure.in.rest;

import com.threepartners.oncologia.application.auditoria.ConsultarAuditoriaUseCase;
import com.threepartners.oncologia.domain.shared.CriterioPaginacion;
import com.threepartners.oncologia.infrastructure.in.rest.dto.PaginaResponseDto;
import com.threepartners.oncologia.infrastructure.in.rest.dto.auditoria.AuditoriaResponseDto;
import com.threepartners.oncologia.infrastructure.in.rest.mapper.AuditoriaRestMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * Unicamente accesible al rol ADMIN (seccion 13): la restriccion real la
 * aplica @PreAuthorize en ConsultarAuditoriaUseCase, no este controlador.
 */
@RestController
@RequestMapping("/api/v1/auditoria")
@RequiredArgsConstructor
public class AuditoriaController {

    private final ConsultarAuditoriaUseCase consultarAuditoriaUseCase;
    private final AuditoriaRestMapper mapper;

    @GetMapping
    public PaginaResponseDto<AuditoriaResponseDto> buscar(
            @RequestParam(required = false) Long usuarioId,
            @RequestParam(required = false) String entidadAfectada,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant hasta,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        // Lo mas reciente primero: es lo que se busca al revisar una bitacora.
        var criterio = new CriterioPaginacion(page, size, "fecha", false);
        var pagina = consultarAuditoriaUseCase.ejecutar(usuarioId, entidadAfectada, desde, hasta, criterio);
        return PaginaResponseDto.de(pagina, mapper::aResponse);
    }
}

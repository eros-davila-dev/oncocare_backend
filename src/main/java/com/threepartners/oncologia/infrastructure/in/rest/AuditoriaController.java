package com.threepartners.oncologia.infrastructure.in.rest;

import com.threepartners.oncologia.infrastructure.in.rest.mapper.NombresVista;
import com.threepartners.oncologia.application.comun.NombresService;
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

    private final NombresService nombresService;

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
        // Quien y sobre que, con nombres: la bitacora la lee una persona, no un sistema.
        java.util.Map<String, String> entidades = new java.util.HashMap<>();
        pagina.contenido().forEach(a -> {
            String descripcion = nombresService.entidad(a.getEntidadAfectada(), a.getEntidadId());
            if (descripcion != null) {
                entidades.put(a.getEntidadAfectada() + ":" + a.getEntidadId(), descripcion);
            }
        });
        var nombres = new NombresVista(null,
                nombresService.usuarios(pagina.contenido().stream().map(a -> a.getUsuarioId()).toList()), entidades);
        return PaginaResponseDto.de(pagina, a -> mapper.aResponse(a, nombres));
    }
}

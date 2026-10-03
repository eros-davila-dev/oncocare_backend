package com.threepartners.oncologia.infrastructure.in.rest;

import com.threepartners.oncologia.application.chatbot.ConfigurarGeminiUseCase;
import com.threepartners.oncologia.infrastructure.in.rest.dto.chatbot.ConfiguracionGeminiDtos;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Configuracion del asistente (Gemini) desde la intranet; solo ADMIN (ver
 * ConfigurarGeminiUseCase). Listar modelos y probar son POST para que la
 * clave viaje en el cuerpo y nunca en la URL (que queda en logs y proxies).
 */
@RestController
@RequestMapping("/api/v1/configuracion/gemini")
@RequiredArgsConstructor
public class ConfiguracionGeminiController {

    private final ConfigurarGeminiUseCase useCase;

    @GetMapping
    public ConfiguracionGeminiDtos.Respuesta consultar() {
        return aDto(useCase.consultar());
    }

    @PutMapping
    public ConfiguracionGeminiDtos.Respuesta actualizar(@Valid @RequestBody ConfiguracionGeminiDtos.Actualizar dto,
                                                        HttpServletRequest request) {
        useCase.actualizar(new ConfigurarGeminiUseCase.Cambios(dto.apiKey(), dto.eliminarClave(), dto.modelos()),
                AutenticacionActual.usuarioId(), AutenticacionActual.ipOrigen(request));
        return aDto(useCase.consultar());
    }

    @PostMapping("/modelos-disponibles")
    public List<ConfiguracionGeminiDtos.ModeloDisponible> modelosDisponibles(
            @Valid @RequestBody(required = false) ConfiguracionGeminiDtos.ListarModelos dto) {
        return useCase.modelosDisponibles(dto != null ? dto.apiKey() : null).stream()
                .map(m -> new ConfiguracionGeminiDtos.ModeloDisponible(m.id(), m.nombre(), m.descripcion()))
                .toList();
    }

    @PostMapping("/probar")
    public ConfiguracionGeminiDtos.ResultadoPrueba probar(@Valid @RequestBody ConfiguracionGeminiDtos.Probar dto) {
        var r = useCase.probar(dto.apiKey(), dto.modelo());
        return new ConfiguracionGeminiDtos.ResultadoPrueba(r.exitoso(), r.modelo(), r.milisegundos(), r.mensaje());
    }

    private static ConfiguracionGeminiDtos.Respuesta aDto(ConfigurarGeminiUseCase.Vista v) {
        var vigente = v.vigente();
        var modelos = v.estadoModelos().stream()
                .map(e -> new ConfiguracionGeminiDtos.EstadoModelo(e.modelo(), e.disponible(), e.apartadoHasta(), e.motivo()))
                .toList();
        return new ConfiguracionGeminiDtos.Respuesta(vigente.origenClave(), vigente.claveEnmascarada(),
                vigente.claveGuardadaIlegible(), vigente.origenModelos(), modelos, v.actualizadoPor(), v.actualizadoEn());
    }
}

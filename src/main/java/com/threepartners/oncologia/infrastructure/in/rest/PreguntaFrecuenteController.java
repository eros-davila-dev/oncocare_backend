package com.threepartners.oncologia.infrastructure.in.rest;

import com.threepartners.oncologia.application.chatbot.GestionarPreguntasFrecuentesUseCase;
import com.threepartners.oncologia.domain.chatbot.PreguntaFrecuente;
import com.threepartners.oncologia.infrastructure.in.rest.dto.chatbot.PreguntaFrecuenteDto;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Base de conocimiento del chatbot y de la pagina de preguntas frecuentes
 * del portal. La lectura de las activas es publica; la edicion, del personal.
 */
@RestController
@RequestMapping("/api/v1/preguntas-frecuentes")
@RequiredArgsConstructor
public class PreguntaFrecuenteController {

    private final GestionarPreguntasFrecuentesUseCase useCase;

    @GetMapping
    public List<PreguntaFrecuenteDto> activas() {
        return useCase.activas().stream().map(PreguntaFrecuenteController::aDto).toList();
    }

    @GetMapping("/todas")
    public List<PreguntaFrecuenteDto> todas() {
        return useCase.todas().stream().map(PreguntaFrecuenteController::aDto).toList();
    }

    @PostMapping
    public ResponseEntity<PreguntaFrecuenteDto> crear(@Valid @RequestBody PreguntaFrecuenteDto dto, HttpServletRequest request) {
        var guardada = useCase.guardar(null, aDominio(dto), AutenticacionActual.usuarioId(), AutenticacionActual.ipOrigen(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(aDto(guardada));
    }

    @PutMapping("/{id}")
    public PreguntaFrecuenteDto actualizar(@PathVariable Long id, @Valid @RequestBody PreguntaFrecuenteDto dto,
                                           HttpServletRequest request) {
        return aDto(useCase.guardar(id, aDominio(dto), AutenticacionActual.usuarioId(), AutenticacionActual.ipOrigen(request)));
    }

    private static PreguntaFrecuente aDominio(PreguntaFrecuenteDto dto) {
        return PreguntaFrecuente.builder()
                .pregunta(dto.pregunta())
                .respuesta(dto.respuesta())
                .categoria(dto.categoria() == null || dto.categoria().isBlank() ? "GENERAL" : dto.categoria().strip())
                .orden(dto.orden())
                .activa(dto.activa())
                .build();
    }

    private static PreguntaFrecuenteDto aDto(PreguntaFrecuente p) {
        return new PreguntaFrecuenteDto(p.getId(), p.getPregunta(), p.getRespuesta(), p.getCategoria(), p.getOrden(), p.isActiva());
    }
}

package com.threepartners.oncologia.infrastructure.in.rest;

import com.threepartners.oncologia.application.recordatorio.LlamadasRecordatorioUseCase;
import com.threepartners.oncologia.application.recordatorio.LlamadasRecordatorioUseCase.LlamadaPendiente;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Recordatorios por llamada para los pacientes sin Telegram (recepcion).
 */
@RestController
@RequestMapping("/api/v1/recordatorios")
@RequiredArgsConstructor
public class RecordatorioController {

    private final LlamadasRecordatorioUseCase llamadasRecordatorioUseCase;

    @GetMapping("/llamadas")
    public List<LlamadaPendiente> llamadasPendientes() {
        return llamadasRecordatorioUseCase.pendientes();
    }

    @PatchMapping("/{id}/llamada")
    public ResponseEntity<Void> registrarLlamada(@PathVariable Long id, @Valid @RequestBody ResultadoLlamadaDto dto,
                                                 HttpServletRequest request) {
        llamadasRecordatorioUseCase.registrarLlamada(id, dto.contesto(), AutenticacionActual.usuarioId(),
                AutenticacionActual.ipOrigen(request));
        return ResponseEntity.noContent().build();
    }

    public record ResultadoLlamadaDto(@NotNull Boolean contesto) {
    }
}

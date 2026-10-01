package com.threepartners.oncologia.infrastructure.in.rest;

import com.threepartners.oncologia.application.documento.AdjuntarDocumentoPacienteUseCase;
import com.threepartners.oncologia.domain.documento.DocumentoPaciente;
import com.threepartners.oncologia.infrastructure.in.rest.dto.documento.DocumentoRequestDto;
import com.threepartners.oncologia.infrastructure.in.rest.dto.documento.DocumentoResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/documentos")
@RequiredArgsConstructor
public class DocumentoController {

    private final AdjuntarDocumentoPacienteUseCase adjuntarDocumentoPacienteUseCase;

    @PostMapping
    public ResponseEntity<DocumentoResponseDto> adjuntar(@Valid @RequestBody DocumentoRequestDto dto) {
        var documento = adjuntarDocumentoPacienteUseCase.ejecutar(DocumentoPaciente.builder()
                .pacienteId(dto.pacienteId())
                .tipoDocumento(dto.tipoDocumento())
                .urlArchivo(dto.urlArchivo())
                .build());
        return ResponseEntity.status(HttpStatus.CREATED).body(aResponse(documento));
    }

    @GetMapping("/paciente/{pacienteId}")
    public List<DocumentoResponseDto> porPaciente(@PathVariable Long pacienteId) {
        return adjuntarDocumentoPacienteUseCase.listarPorPaciente(pacienteId).stream().map(this::aResponse).toList();
    }

    private DocumentoResponseDto aResponse(DocumentoPaciente documento) {
        return new DocumentoResponseDto(documento.getId(), documento.getPacienteId(), documento.getTipoDocumento(),
                documento.getUrlArchivo(), documento.getFechaCarga());
    }
}

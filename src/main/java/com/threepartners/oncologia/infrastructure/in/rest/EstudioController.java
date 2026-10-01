package com.threepartners.oncologia.infrastructure.in.rest;

import com.threepartners.oncologia.application.estudio.GestionarFasesEstudioUseCase;
import com.threepartners.oncologia.application.estudio.GestionarParticipantesUseCase;
import com.threepartners.oncologia.domain.estudio.Fase;
import com.threepartners.oncologia.infrastructure.in.rest.dto.estudio.ConfigurarFaseRequestDto;
import com.threepartners.oncologia.infrastructure.in.rest.dto.estudio.ExcluirParticipanteRequestDto;
import com.threepartners.oncologia.infrastructure.in.rest.dto.estudio.FaseEstudioResponseDto;
import com.threepartners.oncologia.infrastructure.in.rest.dto.estudio.IncluirParticipanteRequestDto;
import com.threepartners.oncologia.infrastructure.in.rest.dto.estudio.ParticipanteResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Configuracion del estudio de tesis: fases pretest/postest y muestra de
 * participantes.
 */
@RestController
@RequestMapping("/api/v1/estudio")
@RequiredArgsConstructor
public class EstudioController {

    private final GestionarFasesEstudioUseCase gestionarFasesUseCase;
    private final GestionarParticipantesUseCase gestionarParticipantesUseCase;

    @GetMapping("/fases")
    public List<FaseEstudioResponseDto> fases() {
        return gestionarFasesUseCase.listar().stream().map(FaseEstudioResponseDto::de).toList();
    }

    @PutMapping("/fases/{fase}")
    public FaseEstudioResponseDto configurarFase(@PathVariable Fase fase, @Valid @RequestBody ConfigurarFaseRequestDto dto,
                                                 HttpServletRequest request) {
        return FaseEstudioResponseDto.de(gestionarFasesUseCase.configurar(fase, dto.fechaInicio(), dto.fechaFin(),
                AutenticacionActual.usuarioId(), AutenticacionActual.ipOrigen(request)));
    }

    @PostMapping("/fases/{fase}/cerrar")
    public FaseEstudioResponseDto cerrarFase(@PathVariable Fase fase, HttpServletRequest request) {
        return FaseEstudioResponseDto.de(gestionarFasesUseCase.cerrar(fase,
                AutenticacionActual.usuarioId(), AutenticacionActual.ipOrigen(request)));
    }

    @GetMapping("/participantes")
    public List<ParticipanteResponseDto> participantes() {
        return gestionarParticipantesUseCase.listar().stream().map(ParticipanteResponseDto::de).toList();
    }

    @PostMapping("/participantes")
    public ResponseEntity<ParticipanteResponseDto> incluir(@Valid @RequestBody IncluirParticipanteRequestDto dto,
                                                           HttpServletRequest request) {
        var participante = gestionarParticipantesUseCase.incluir(dto.pacienteId(), dto.fechaConsentimiento(),
                dto.observacion(), AutenticacionActual.usuarioId(), AutenticacionActual.ipOrigen(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(ParticipanteResponseDto.de(participante));
    }

    @PatchMapping("/participantes/{id}/excluir")
    public ParticipanteResponseDto excluir(@PathVariable Long id, @Valid @RequestBody ExcluirParticipanteRequestDto dto,
                                           HttpServletRequest request) {
        return ParticipanteResponseDto.de(gestionarParticipantesUseCase.excluir(id, dto.motivo(), dto.observacion(),
                AutenticacionActual.usuarioId(), AutenticacionActual.ipOrigen(request)));
    }

    @PatchMapping("/participantes/{id}/reincorporar")
    public ParticipanteResponseDto reincorporar(@PathVariable Long id, HttpServletRequest request) {
        return ParticipanteResponseDto.de(gestionarParticipantesUseCase.reincorporar(id,
                AutenticacionActual.usuarioId(), AutenticacionActual.ipOrigen(request)));
    }
}

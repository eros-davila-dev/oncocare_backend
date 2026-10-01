package com.threepartners.oncologia.infrastructure.in.rest;

import com.threepartners.oncologia.application.chatbot.BandejaConsultasUseCase;
import com.threepartners.oncologia.domain.shared.CriterioPaginacion;
import com.threepartners.oncologia.infrastructure.in.rest.dto.PaginaResponseDto;
import com.threepartners.oncologia.infrastructure.in.rest.dto.chatbot.ResolverConsultaRequestDto;
import com.threepartners.oncologia.infrastructure.in.rest.dto.chatbot.TurnoConversacionDto;
import com.threepartners.oncologia.infrastructure.in.rest.dto.estudio.ConsultaResponseDto;
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

import java.util.List;

/**
 * Bandeja del personal: consultas escaladas por el chatbot (indicador NCA).
 */
@RestController
@RequestMapping("/api/v1/consultas")
@RequiredArgsConstructor
public class ConsultaController {

    private final BandejaConsultasUseCase bandejaConsultasUseCase;

    @GetMapping("/bandeja")
    public PaginaResponseDto<ConsultaResponseDto> bandeja(@RequestParam(defaultValue = "0") int page,
                                                         @RequestParam(defaultValue = "20") int size) {
        return PaginaResponseDto.de(bandejaConsultasUseCase.escaladas(CriterioPaginacion.de(page, size)), ConsultaResponseDto::de);
    }

    @GetMapping("/{id}/conversacion")
    public List<TurnoConversacionDto> conversacion(@PathVariable Long id) {
        return bandejaConsultasUseCase.conversacion(id).stream()
                .map(t -> new TurnoConversacionDto(t.getFecha(), t.getMensajeUsuario(), t.getRespuestaBot(), t.getIntencionDetectada()))
                .toList();
    }

    @PatchMapping("/{id}/resolver")
    public ConsultaResponseDto resolver(@PathVariable Long id, @Valid @RequestBody ResolverConsultaRequestDto dto,
                                        HttpServletRequest request) {
        return ConsultaResponseDto.de(bandejaConsultasUseCase.resolver(id, dto.resuelta(), dto.nota(),
                AutenticacionActual.usuarioId(), AutenticacionActual.ipOrigen(request)));
    }
}

package com.threepartners.oncologia.infrastructure.in.rest;

import com.threepartners.oncologia.application.chatbot.BandejaConsultasUseCase;
import com.threepartners.oncologia.application.chatbot.HistorialConsultasUseCase;
import com.threepartners.oncologia.domain.chatbot.ConversacionChatbot;
import com.threepartners.oncologia.domain.shared.CriterioPaginacion;
import com.threepartners.oncologia.infrastructure.in.rest.dto.PaginaResponseDto;
import com.threepartners.oncologia.infrastructure.in.rest.dto.chatbot.ResolverConsultaRequestDto;
import com.threepartners.oncologia.infrastructure.in.rest.dto.chatbot.TurnoConversacionDto;
import com.threepartners.oncologia.infrastructure.in.rest.dto.estudio.ConsultaResponseDto;
import com.threepartners.oncologia.infrastructure.in.rest.dto.estudio.HistorialConsultaDto;
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
 * Bandeja del personal (consultas escaladas por el chatbot, indicador NCA) e
 * historial de consultas: "Mis consultas" del paciente y el de cada paciente
 * en su ficha.
 */
@RestController
@RequestMapping("/api/v1/consultas")
@RequiredArgsConstructor
public class ConsultaController {

    private final BandejaConsultasUseCase bandejaConsultasUseCase;
    private final HistorialConsultasUseCase historialConsultasUseCase;

    @GetMapping("/bandeja")
    public PaginaResponseDto<ConsultaResponseDto> bandeja(@RequestParam(defaultValue = "0") int page,
                                                         @RequestParam(defaultValue = "20") int size) {
        return PaginaResponseDto.de(bandejaConsultasUseCase.escaladas(CriterioPaginacion.de(page, size)),
                e -> ConsultaResponseDto.deBandeja(e.consulta(), e.pacienteNombre(), e.pacienteTelefono()));
    }

    @GetMapping("/{id}/conversacion")
    public List<TurnoConversacionDto> conversacion(@PathVariable Long id) {
        return bandejaConsultasUseCase.conversacion(id).stream()
                .map(ConsultaController::turno)
                .toList();
    }

    /** Consultas del paciente autenticado, de la mas reciente a la mas antigua. */
    @GetMapping("/mias")
    public PaginaResponseDto<HistorialConsultaDto> misConsultas(@RequestParam(defaultValue = "0") int page,
                                                              @RequestParam(defaultValue = "20") int size) {
        return PaginaResponseDto.de(historialConsultasUseCase.misConsultas(AutenticacionActual.usuarioId(),
                CriterioPaginacion.de(page, size)), HistorialConsultaDto::de);
    }

    @GetMapping("/mias/{id}/conversacion")
    public List<TurnoConversacionDto> miConversacion(@PathVariable Long id) {
        return historialConsultasUseCase.miConversacion(AutenticacionActual.usuarioId(), id).stream()
                .map(ConsultaController::turno)
                .toList();
    }

    /** Historial de un paciente (ficha en la intranet). */
    @GetMapping("/paciente/{pacienteId}")
    public PaginaResponseDto<HistorialConsultaDto> dePaciente(@PathVariable Long pacienteId,
                                                             @RequestParam(defaultValue = "0") int page,
                                                             @RequestParam(defaultValue = "20") int size) {
        return PaginaResponseDto.de(historialConsultasUseCase.dePaciente(pacienteId, CriterioPaginacion.de(page, size)),
                HistorialConsultaDto::de);
    }

    private static TurnoConversacionDto turno(ConversacionChatbot t) {
        return new TurnoConversacionDto(t.getFecha(), t.getMensajeUsuario(), t.getRespuestaBot(), t.getIntencionDetectada());
    }

    @PatchMapping("/{id}/resolver")
    public ConsultaResponseDto resolver(@PathVariable Long id, @Valid @RequestBody ResolverConsultaRequestDto dto,
                                        HttpServletRequest request) {
        return ConsultaResponseDto.de(bandejaConsultasUseCase.resolver(id, dto.resuelta(), dto.nota(),
                AutenticacionActual.usuarioId(), AutenticacionActual.ipOrigen(request)));
    }
}

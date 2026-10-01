package com.threepartners.oncologia.infrastructure.in.rest;

import com.threepartners.oncologia.application.cita.AgendarCitaUseCase;
import com.threepartners.oncologia.application.cita.CancelarCitaUseCase;
import com.threepartners.oncologia.application.cita.ConsultarAgendaUseCase;
import com.threepartners.oncologia.application.cita.ConsultarCitaUseCase;
import com.threepartners.oncologia.application.cita.ListarCitasProximasUseCase;
import com.threepartners.oncologia.application.cita.RegistrarAsistenciaCitaUseCase;
import com.threepartners.oncologia.application.cita.ReprogramarCitaUseCase;
import com.threepartners.oncologia.domain.cita.CitaAgenda;
import com.threepartners.oncologia.domain.cita.EstadoCita;
import com.threepartners.oncologia.domain.shared.CriterioPaginacion;
import com.threepartners.oncologia.infrastructure.in.rest.dto.PaginaResponseDto;
import com.threepartners.oncologia.infrastructure.in.rest.dto.cita.CancelarCitaRequestDto;
import com.threepartners.oncologia.infrastructure.in.rest.dto.cita.CitaAgendaResponseDto;
import com.threepartners.oncologia.infrastructure.in.rest.dto.cita.CorregirDesenlaceRequestDto;
import com.threepartners.oncologia.infrastructure.in.rest.dto.cita.CitaRequestDto;
import com.threepartners.oncologia.infrastructure.in.rest.dto.cita.CitaResponseDto;
import com.threepartners.oncologia.infrastructure.in.rest.dto.cita.ReprogramarCitaRequestDto;
import com.threepartners.oncologia.infrastructure.in.rest.mapper.CitaRestMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/citas")
@RequiredArgsConstructor
public class CitaController {

    private final AgendarCitaUseCase agendarCitaUseCase;
    private final ReprogramarCitaUseCase reprogramarCitaUseCase;
    private final CancelarCitaUseCase cancelarCitaUseCase;
    private final RegistrarAsistenciaCitaUseCase registrarAsistenciaCitaUseCase;
    private final ConsultarCitaUseCase consultarCitaUseCase;
    private final ListarCitasProximasUseCase listarCitasProximasUseCase;
    private final ConsultarAgendaUseCase consultarAgendaUseCase;
    private final CitaRestMapper mapper;

    @PostMapping
    public ResponseEntity<CitaResponseDto> agendar(@Valid @RequestBody CitaRequestDto dto, HttpServletRequest request) {
        var cita = agendarCitaUseCase.ejecutar(mapper.aDominio(dto), dto.medicionId(),
                AutenticacionActual.usuarioId(), AutenticacionActual.rol(), AutenticacionActual.ipOrigen(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.aResponse(cita));
    }

    @PatchMapping("/{id}/reprogramar")
    public CitaResponseDto reprogramar(@PathVariable Long id, @Valid @RequestBody ReprogramarCitaRequestDto dto, HttpServletRequest request) {
        var cita = reprogramarCitaUseCase.ejecutar(id, dto.fecha(), dto.hora(),
                AutenticacionActual.usuarioId(), AutenticacionActual.rol(), AutenticacionActual.ipOrigen(request));
        return mapper.aResponse(cita);
    }

    @PatchMapping("/{id}/cancelar")
    public CitaResponseDto cancelar(@PathVariable Long id, @Valid @RequestBody CancelarCitaRequestDto dto, HttpServletRequest request) {
        var cita = cancelarCitaUseCase.ejecutar(id, dto.motivo(),
                AutenticacionActual.usuarioId(), AutenticacionActual.rol(), AutenticacionActual.ipOrigen(request));
        return mapper.aResponse(cita);
    }

    @PatchMapping("/{id}/confirmar")
    public CitaResponseDto confirmar(@PathVariable Long id) {
        return mapper.aResponse(registrarAsistenciaCitaUseCase.confirmar(id, AutenticacionActual.usuarioId(), AutenticacionActual.rol()));
    }

    @PatchMapping("/{id}/atender")
    public CitaResponseDto atender(@PathVariable Long id, HttpServletRequest request) {
        return mapper.aResponse(registrarAsistenciaCitaUseCase.marcarAtendida(
                id, AutenticacionActual.usuarioId(), AutenticacionActual.ipOrigen(request)));
    }

    @PatchMapping("/{id}/no-asistio")
    public CitaResponseDto marcarNoAsistio(@PathVariable Long id, HttpServletRequest request) {
        return mapper.aResponse(registrarAsistenciaCitaUseCase.marcarNoAsistio(
                id, AutenticacionActual.usuarioId(), AutenticacionActual.ipOrigen(request)));
    }

    @PatchMapping("/{id}/corregir-desenlace")
    public CitaResponseDto corregirDesenlace(@PathVariable Long id, @Valid @RequestBody CorregirDesenlaceRequestDto dto,
                                             HttpServletRequest request) {
        return mapper.aResponse(registrarAsistenciaCitaUseCase.corregirDesenlace(id, dto.estado(), dto.motivo(),
                AutenticacionActual.usuarioId(), AutenticacionActual.ipOrigen(request)));
    }

    /**
     * Agenda del dia para recepcion: donde se registra si el paciente llego
     * (indicador TNS). Un medico solo ve sus citas.
     */
    @GetMapping("/agenda")
    public List<CitaAgendaResponseDto> agenda(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) Long medicoId) {
        return consultarAgendaUseCase.delDia(fecha, medicoId, AutenticacionActual.usuarioId(), AutenticacionActual.rol())
                .stream().map(this::aAgendaResponse).toList();
    }

    @GetMapping("/pendientes-cierre")
    public List<CitaAgendaResponseDto> pendientesDeCierre() {
        return consultarAgendaUseCase.pendientesDeCierre(AutenticacionActual.usuarioId(), AutenticacionActual.rol())
                .stream().map(this::aAgendaResponse).toList();
    }

    private CitaAgendaResponseDto aAgendaResponse(CitaAgenda c) {
        return new CitaAgendaResponseDto(mapper.aResponse(c.cita()), c.pacienteNombre(), c.pacienteDocumento(),
                c.pacienteTelefono(), c.medicoNombre());
    }

    /**
     * Citas del paciente autenticado (seccion 12): usado por el portal y el
     * chatbot para "identificar al usuario autenticado" en vez de exponer un
     * id de cita/paciente arbitrario.
     */
    @GetMapping("/mias")
    public PaginaResponseDto<CitaResponseDto> misCitas(
            @RequestParam(required = false) EstadoCita estado,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        var pagina = consultarCitaUseCase.misCitas(AutenticacionActual.usuarioId(), estado, CriterioPaginacion.de(page, size));
        return PaginaResponseDto.de(pagina, mapper::aResponse);
    }

    @GetMapping("/{id}")
    public CitaResponseDto porId(@PathVariable Long id) {
        return mapper.aResponse(consultarCitaUseCase.porId(id));
    }

    @GetMapping
    public PaginaResponseDto<CitaResponseDto> listar(
            @RequestParam(required = false) Long pacienteId,
            @RequestParam(required = false) Long medicoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) EstadoCita estado,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        var pagina = consultarCitaUseCase.listar(pacienteId, medicoId, desde, hasta, estado, CriterioPaginacion.de(page, size));
        return PaginaResponseDto.de(pagina, mapper::aResponse);
    }

    /**
     * Consumido por el workflow n8n de recordatorios (seccion 15, flujo 2).
     */
    @GetMapping("/proximas")
    public List<CitaResponseDto> proximas(
            @RequestParam(defaultValue = "24") int horasDesde,
            @RequestParam(defaultValue = "48") int horasHasta) {
        return listarCitasProximasUseCase.ejecutar(horasDesde, horasHasta).stream().map(mapper::aResponse).toList();
    }
}

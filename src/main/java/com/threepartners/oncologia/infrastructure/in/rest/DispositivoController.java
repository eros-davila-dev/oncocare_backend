package com.threepartners.oncologia.infrastructure.in.rest;

import com.threepartners.oncologia.application.dispositivo.GestionarDispositivoUseCase;
import com.threepartners.oncologia.domain.dispositivo.DispositivoExterno;
import com.threepartners.oncologia.domain.dispositivo.LecturaDispositivo;
import com.threepartners.oncologia.infrastructure.in.rest.dto.dispositivo.DispositivoRequestDto;
import com.threepartners.oncologia.infrastructure.in.rest.dto.dispositivo.DispositivoResponseDto;
import com.threepartners.oncologia.infrastructure.in.rest.dto.dispositivo.LecturaDispositivoResponseDto;
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

/**
 * Administracion de dispositivos externos (seccion 14). El registro de
 * lecturas en si no pasa por aqui: llega por los adaptadores de entrada
 * (DeviceWebhookController, MqttDeviceListenerAdapter).
 */
@RestController
@RequestMapping("/api/v1/dispositivos")
@RequiredArgsConstructor
public class DispositivoController {

    private final GestionarDispositivoUseCase gestionarDispositivoUseCase;
    private final CredencialDispositivoHasher credencialHasher;

    @PostMapping
    public ResponseEntity<DispositivoResponseDto> registrar(@Valid @RequestBody DispositivoRequestDto dto) {
        var dispositivo = gestionarDispositivoUseCase.registrar(DispositivoExterno.builder()
                .nombre(dto.nombre())
                .tipo(dto.tipo())
                .protocolo(dto.protocolo())
                .credencialHash(credencialHasher.hash(dto.credencial()))
                .build());

        return ResponseEntity.status(HttpStatus.CREATED).body(aResponse(dispositivo));
    }

    @GetMapping
    public List<DispositivoResponseDto> listar() {
        return gestionarDispositivoUseCase.listar().stream().map(this::aResponse).toList();
    }

    @GetMapping("/lecturas/paciente/{pacienteId}")
    public List<LecturaDispositivoResponseDto> lecturasPorPaciente(@PathVariable Long pacienteId) {
        return gestionarDispositivoUseCase.lecturasPorPaciente(pacienteId).stream().map(this::aResponse).toList();
    }

    private DispositivoResponseDto aResponse(DispositivoExterno dispositivo) {
        return new DispositivoResponseDto(dispositivo.getId(), dispositivo.getNombre(), dispositivo.getTipo(),
                dispositivo.getProtocolo(), dispositivo.getEstadoConexion());
    }

    private LecturaDispositivoResponseDto aResponse(LecturaDispositivo lectura) {
        return new LecturaDispositivoResponseDto(lectura.getId(), lectura.getDispositivoId(), lectura.getPacienteId(),
                lectura.getCicloTratamientoId(), lectura.getTipoDato(), lectura.getValor(), lectura.getFechaLectura());
    }
}

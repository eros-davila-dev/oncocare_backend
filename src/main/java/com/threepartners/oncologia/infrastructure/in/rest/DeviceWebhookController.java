package com.threepartners.oncologia.infrastructure.in.rest;

import com.threepartners.oncologia.domain.dispositivo.DispositivoLecturaPort;
import com.threepartners.oncologia.domain.dispositivo.DispositivoRepositoryPort;
import com.threepartners.oncologia.domain.dispositivo.LecturaDispositivo;
import com.threepartners.oncologia.domain.shared.exception.CredencialesInvalidasException;
import com.threepartners.oncologia.infrastructure.in.rest.dto.dispositivo.LecturaDispositivoRequestDto;
import com.threepartners.oncologia.infrastructure.in.rest.dto.dispositivo.LecturaDispositivoResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adaptador de entrada REST para equipos que pueden hacer una peticion HTTP
 * directa (seccion 14). Se autentica con la credencial propia del dispositivo
 * (header X-Device-Credential), nunca con el JWT de un usuario humano. Al
 * igual que MqttDeviceListenerAdapter, converge en el mismo puerto de dominio
 * DispositivoLecturaPort sin que el dominio conozca este protocolo.
 */
@RestController
@RequestMapping("/api/v1/devices/webhook")
@RequiredArgsConstructor
public class DeviceWebhookController {

    private static final String HEADER_CREDENCIAL = "X-Device-Credential";

    private final DispositivoRepositoryPort dispositivoRepositoryPort;
    private final DispositivoLecturaPort dispositivoLecturaPort;
    private final CredencialDispositivoHasher credencialHasher;

    @PostMapping("/lecturas")
    public ResponseEntity<LecturaDispositivoResponseDto> registrarLectura(
            @RequestHeader(value = HEADER_CREDENCIAL, required = false) String credencial,
            @Valid @RequestBody LecturaDispositivoRequestDto dto) {

        var dispositivo = autenticarDispositivo(credencial);

        var lectura = dispositivoLecturaPort.registrarLectura(LecturaDispositivo.builder()
                .dispositivoId(dispositivo.getId())
                .pacienteId(dto.pacienteId())
                .cicloTratamientoId(dto.cicloTratamientoId())
                .tipoDato(dto.tipoDato())
                .valor(dto.valor())
                .build());

        return ResponseEntity.status(HttpStatus.CREATED).body(new LecturaDispositivoResponseDto(
                lectura.getId(), lectura.getDispositivoId(), lectura.getPacienteId(),
                lectura.getCicloTratamientoId(), lectura.getTipoDato(), lectura.getValor(), lectura.getFechaLectura()));
    }

    private com.threepartners.oncologia.domain.dispositivo.DispositivoExterno autenticarDispositivo(String credencial) {
        if (credencial == null || credencial.isBlank()) {
            throw new CredencialesInvalidasException();
        }
        return dispositivoRepositoryPort.buscarPorCredencialHash(credencialHasher.hash(credencial))
                .orElseThrow(CredencialesInvalidasException::new);
    }
}

package com.threepartners.oncologia.infrastructure.in.rest;

import com.threepartners.oncologia.application.estudio.IniciarMedicionRegistroUseCase;
import com.threepartners.oncologia.infrastructure.in.rest.dto.estudio.IniciarMedicionRequestDto;
import com.threepartners.oncologia.infrastructure.in.rest.dto.estudio.IniciarMedicionResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/mediciones")
@RequiredArgsConstructor
public class MedicionController {

    private final IniciarMedicionRegistroUseCase iniciarMedicionRegistroUseCase;

    @Operation(summary = "Abre una sesion de medicion del tiempo de registro (indicador TPR)",
            description = "Llamar al mostrar el formulario de registro y reenviar el medicionId al guardar. "
                    + "El servidor sella inicio y fin; el cliente nunca envia una duracion.")
    @PostMapping("/registro")
    public ResponseEntity<IniciarMedicionResponseDto> iniciar(@Valid @RequestBody IniciarMedicionRequestDto dto) {
        var medicion = iniciarMedicionRegistroUseCase.ejecutar(dto.tipo(), AutenticacionActual.usuarioId(), AutenticacionActual.rol());
        return ResponseEntity.status(HttpStatus.CREATED).body(new IniciarMedicionResponseDto(
                medicion.getId(), medicion.getTipo(), medicion.getCanal(), medicion.getInicio()));
    }
}

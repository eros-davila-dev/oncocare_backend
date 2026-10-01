package com.threepartners.oncologia.infrastructure.in.rest.dto.dispositivo;

import com.threepartners.oncologia.domain.dispositivo.ProtocoloDispositivo;
import com.threepartners.oncologia.domain.dispositivo.TipoDispositivo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DispositivoRequestDto(
        @NotBlank(message = "El nombre es obligatorio")
        String nombre,

        @NotNull(message = "El tipo de dispositivo es obligatorio")
        TipoDispositivo tipo,

        @NotNull(message = "El protocolo es obligatorio")
        ProtocoloDispositivo protocolo,

        @NotBlank(message = "La credencial es obligatoria")
        @Size(min = 16, message = "La credencial debe tener al menos 16 caracteres")
        String credencial
) {
}

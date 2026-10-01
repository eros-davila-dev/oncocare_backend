package com.threepartners.oncologia.infrastructure.in.rest.dto.estudio;

import com.threepartners.oncologia.domain.estudio.TipoMedicion;
import jakarta.validation.constraints.NotNull;

public record IniciarMedicionRequestDto(

        @NotNull(message = "El tipo de registro es obligatorio")
        TipoMedicion tipo
) {
}

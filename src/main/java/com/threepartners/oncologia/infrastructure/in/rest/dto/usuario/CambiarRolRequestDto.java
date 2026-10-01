package com.threepartners.oncologia.infrastructure.in.rest.dto.usuario;

import com.threepartners.oncologia.domain.usuario.Rol;
import jakarta.validation.constraints.NotNull;

public record CambiarRolRequestDto(
        @NotNull(message = "El nuevo rol es obligatorio")
        Rol rol
) {
}

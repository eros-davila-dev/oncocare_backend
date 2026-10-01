package com.threepartners.oncologia.infrastructure.in.rest.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record VerificarEmailRequestDto(
        @NotBlank(message = "El token es obligatorio")
        String token
) {
}

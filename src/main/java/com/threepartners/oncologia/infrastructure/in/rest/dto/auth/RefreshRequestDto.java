package com.threepartners.oncologia.infrastructure.in.rest.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequestDto(
        @NotBlank(message = "El refresh token es obligatorio")
        String refreshToken
) {
}

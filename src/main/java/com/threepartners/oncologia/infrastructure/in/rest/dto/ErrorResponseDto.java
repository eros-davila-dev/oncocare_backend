package com.threepartners.oncologia.infrastructure.in.rest.dto;

import java.time.Instant;
import java.util.List;

public record ErrorResponseDto(
        Instant timestamp,
        int status,
        String code,
        String message,
        String path,
        List<CampoErrorDto> errores
) {

    public record CampoErrorDto(String campo, String mensaje) {
    }

    public static ErrorResponseDto de(int status, String code, String message, String path) {
        return new ErrorResponseDto(Instant.now(), status, code, message, path, List.of());
    }

    public static ErrorResponseDto conErrores(int status, String code, String message, String path, List<CampoErrorDto> errores) {
        return new ErrorResponseDto(Instant.now(), status, code, message, path, errores);
    }
}

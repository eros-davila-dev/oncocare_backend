package com.threepartners.oncologia.infrastructure.in.rest.advice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.threepartners.oncologia.infrastructure.in.rest.dto.ErrorResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JsonAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        var body = ErrorResponseDto.de(403, "SIN_PERMISOS", "No tiene permisos para realizar esta accion", request.getRequestURI());
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}

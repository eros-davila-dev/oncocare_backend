package com.threepartners.oncologia.infrastructure.in.rest.advice;

import com.threepartners.oncologia.domain.shared.exception.ConflictoDeNegocioException;
import com.threepartners.oncologia.domain.shared.exception.CredencialesInvalidasException;
import com.threepartners.oncologia.domain.shared.exception.CuentaBloqueadaException;
import com.threepartners.oncologia.domain.shared.exception.CuentaNoVerificadaException;
import com.threepartners.oncologia.domain.shared.exception.RecursoNoEncontradoException;
import com.threepartners.oncologia.domain.shared.exception.RegistroNoDisponibleException;
import com.threepartners.oncologia.domain.shared.exception.TokenInvalidoException;
import com.threepartners.oncologia.domain.shared.exception.ValidacionDeNegocioException;
import com.threepartners.oncologia.infrastructure.in.rest.dto.ErrorResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
 * Catalogo de errores consistente (seccion 16): cada respuesta de error sigue
 * el mismo formato {timestamp, status, code, message, path} para que tanto el
 * frontend como el chatbot (via n8n) puedan interpretarlo de la misma forma.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> manejarValidacion(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ErrorResponseDto.CampoErrorDto> errores = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ErrorResponseDto.CampoErrorDto(fe.getField(), fe.getDefaultMessage()))
                .toList();

        return ResponseEntity.badRequest().body(ErrorResponseDto.conErrores(
                400, "VALIDACION", "Uno o mas campos no son validos", request.getRequestURI(), errores));
    }

    @ExceptionHandler(ValidacionDeNegocioException.class)
    public ResponseEntity<ErrorResponseDto> manejarValidacionNegocio(ValidacionDeNegocioException ex, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(
                ErrorResponseDto.de(400, "REGLA_DE_NEGOCIO", ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<ErrorResponseDto> manejarCredenciales(CredencialesInvalidasException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                ErrorResponseDto.de(401, "NO_AUTENTICADO", ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(CuentaNoVerificadaException.class)
    public ResponseEntity<ErrorResponseDto> manejarCuentaNoVerificada(CuentaNoVerificadaException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                ErrorResponseDto.de(403, "CUENTA_NO_VERIFICADA", ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(CuentaBloqueadaException.class)
    public ResponseEntity<ErrorResponseDto> manejarCuentaBloqueada(CuentaBloqueadaException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.LOCKED).body(
                ErrorResponseDto.de(423, "CUENTA_BLOQUEADA", ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(TokenInvalidoException.class)
    public ResponseEntity<ErrorResponseDto> manejarTokenInvalido(TokenInvalidoException ex, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(
                ErrorResponseDto.de(400, "TOKEN_INVALIDO", ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(RegistroNoDisponibleException.class)
    public ResponseEntity<ErrorResponseDto> manejarRegistroNoDisponible(RegistroNoDisponibleException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
                ErrorResponseDto.de(503, "REGISTRO_NO_DISPONIBLE", ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDto> manejarAccesoDenegado(AccessDeniedException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                ErrorResponseDto.de(403, "SIN_PERMISOS", "No tiene permisos para realizar esta accion", request.getRequestURI()));
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorResponseDto> manejarNoEncontrado(RecursoNoEncontradoException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ErrorResponseDto.de(404, "NO_ENCONTRADO", ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(ConflictoDeNegocioException.class)
    public ResponseEntity<ErrorResponseDto> manejarConflicto(ConflictoDeNegocioException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ErrorResponseDto.de(409, "CONFLICTO", ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> manejarErrorInterno(Exception ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ErrorResponseDto.de(500, "ERROR_INTERNO", "Ocurrio un error inesperado", request.getRequestURI()));
    }
}

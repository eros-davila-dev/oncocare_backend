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
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Catalogo de errores consistente (seccion 16): cada respuesta de error sigue
 * el mismo formato {timestamp, status, code, message, path} para que tanto el
 * frontend como el chatbot (via n8n) puedan interpretarlo de la misma forma.
 */
@Slf4j
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

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponseDto> manejarParametroFaltante(MissingServletRequestParameterException ex,
                                                                     HttpServletRequest request) {
        return solicitudInvalida("Falta el parametro obligatorio '" + ex.getParameterName() + "'", request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponseDto> manejarTipoInvalido(MethodArgumentTypeMismatchException ex,
                                                                HttpServletRequest request) {
        String mensaje = "Valor no valido para '" + ex.getName() + "'";
        Class<?> tipo = ex.getRequiredType();
        if (tipo != null && tipo.isEnum()) {
            mensaje += " (valores permitidos: " + Arrays.stream(tipo.getEnumConstants())
                    .map(String::valueOf).collect(Collectors.joining(", ")) + ")";
        }
        return solicitudInvalida(mensaje, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDto> manejarCuerpoIlegible(HttpMessageNotReadableException ex,
                                                                  HttpServletRequest request) {
        return solicitudInvalida("El cuerpo de la solicitud no es JSON valido o tiene un valor no permitido", request);
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ErrorResponseDto> manejarParteFaltante(MissingServletRequestPartException ex,
                                                                 HttpServletRequest request) {
        return solicitudInvalida("Falta el archivo '" + ex.getRequestPartName() + "'", request);
    }

    /**
     * Las excepciones propias de Spring MVC (ruta inexistente, metodo HTTP o
     * tipo de contenido no soportado, archivo demasiado grande...) traen su
     * propio codigo 4xx: no deben convertirse en un 500. Solo un error de
     * verdad inesperado responde 500, y ese se registra con su traza (la
     * respuesta nunca la expone).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> manejarErrorInterno(Exception ex, HttpServletRequest request) {
        if (ex instanceof ErrorResponse respuestaSpring && respuestaSpring.getStatusCode().is4xxClientError()) {
            int estado = respuestaSpring.getStatusCode().value();
            return ResponseEntity.status(estado).body(ErrorResponseDto.de(estado, "SOLICITUD_INVALIDA",
                    mensajeCliente(respuestaSpring), request.getRequestURI()));
        }
        log.error("Error inesperado en {} {}", request.getMethod(), request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ErrorResponseDto.de(500, "ERROR_INTERNO", "Ocurrio un error inesperado", request.getRequestURI()));
    }

    private static ResponseEntity<ErrorResponseDto> solicitudInvalida(String mensaje, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(ErrorResponseDto.de(400, "SOLICITUD_INVALIDA", mensaje, request.getRequestURI()));
    }

    private static String mensajeCliente(ErrorResponse respuesta) {
        return switch (respuesta.getStatusCode().value()) {
            case 404 -> "El recurso solicitado no existe";
            case 405 -> "Metodo HTTP no permitido para este recurso";
            case 413 -> "El archivo supera el tamano maximo permitido";
            case 415 -> "Tipo de contenido no soportado";
            default -> "Solicitud no valida";
        };
    }
}

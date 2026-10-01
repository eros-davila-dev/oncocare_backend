package com.threepartners.oncologia.infrastructure.in.rest;

import com.threepartners.oncologia.application.auth.AutenticarUsuarioUseCase;
import com.threepartners.oncologia.application.auth.CerrarSesionUseCase;
import com.threepartners.oncologia.application.auth.LoginResultado;
import com.threepartners.oncologia.application.auth.RefrescarTokenUseCase;
import com.threepartners.oncologia.application.auth.RegistrarCuentaPacienteUseCase;
import com.threepartners.oncologia.application.auth.RestablecerPasswordUseCase;
import com.threepartners.oncologia.application.auth.SolicitarRecuperacionPasswordUseCase;
import com.threepartners.oncologia.application.auth.VerificarEmailUseCase;
import com.threepartners.oncologia.infrastructure.in.rest.dto.auth.LoginRequestDto;
import com.threepartners.oncologia.infrastructure.in.rest.dto.auth.LoginResponseDto;
import com.threepartners.oncologia.infrastructure.in.rest.dto.auth.RecuperarPasswordRequestDto;
import com.threepartners.oncologia.infrastructure.in.rest.dto.auth.RefreshRequestDto;
import com.threepartners.oncologia.infrastructure.in.rest.dto.auth.RegistroCuentaRequestDto;
import com.threepartners.oncologia.infrastructure.in.rest.dto.auth.RestablecerPasswordRequestDto;
import com.threepartners.oncologia.infrastructure.in.rest.dto.auth.UsuarioResumenDto;
import com.threepartners.oncologia.infrastructure.in.rest.dto.auth.VerificarEmailRequestDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String MENSAJE_RECUPERACION_GENERICO =
            "Si el correo esta registrado, recibiras un enlace para restablecer tu contrasena";

    private final AutenticarUsuarioUseCase autenticarUsuarioUseCase;
    private final RefrescarTokenUseCase refrescarTokenUseCase;
    private final RegistrarCuentaPacienteUseCase registrarCuentaPacienteUseCase;
    private final VerificarEmailUseCase verificarEmailUseCase;
    private final SolicitarRecuperacionPasswordUseCase solicitarRecuperacionPasswordUseCase;
    private final RestablecerPasswordUseCase restablecerPasswordUseCase;
    private final CerrarSesionUseCase cerrarSesionUseCase;

    @PostMapping("/login")
    public LoginResponseDto login(@Valid @RequestBody LoginRequestDto dto, HttpServletRequest request) {
        LoginResultado resultado = autenticarUsuarioUseCase.ejecutar(dto.email(), dto.password(), AutenticacionActual.ipOrigen(request));
        return aResponse(resultado);
    }

    @PostMapping("/refresh")
    public LoginResponseDto refrescar(@Valid @RequestBody RefreshRequestDto dto, HttpServletRequest request) {
        LoginResultado resultado = refrescarTokenUseCase.ejecutar(dto.refreshToken(), AutenticacionActual.ipOrigen(request));
        return aResponse(resultado);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequestDto dto, HttpServletRequest request) {
        cerrarSesionUseCase.ejecutar(dto.refreshToken(), AutenticacionActual.ipOrigen(request));
        return ResponseEntity.noContent().build();
    }

    /**
     * Registro de autoservicio para pacientes (seccion 7, paso 1). La
     * respuesta es siempre 202 con el mismo mensaje exista o no ya el correo,
     * para no permitir enumerar cuentas registradas (seccion 9).
     */
    @PostMapping("/registro")
    public ResponseEntity<Map<String, String>> registrar(@Valid @RequestBody RegistroCuentaRequestDto dto, HttpServletRequest request) {
        registrarCuentaPacienteUseCase.ejecutar(dto.nombres(), dto.email(), dto.password(), AutenticacionActual.ipOrigen(request));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of(
                "mensaje", "Si los datos son validos, recibiras un correo para verificar tu cuenta"));
    }

    @PostMapping("/verificar-email")
    public ResponseEntity<Void> verificarEmail(@Valid @RequestBody VerificarEmailRequestDto dto, HttpServletRequest request) {
        verificarEmailUseCase.ejecutar(dto.token(), AutenticacionActual.ipOrigen(request));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/recuperar-password")
    public Map<String, String> recuperarPassword(@Valid @RequestBody RecuperarPasswordRequestDto dto) {
        solicitarRecuperacionPasswordUseCase.ejecutar(dto.email());
        return Map.of("mensaje", MENSAJE_RECUPERACION_GENERICO);
    }

    @PostMapping("/restablecer-password")
    public ResponseEntity<Void> restablecerPassword(@Valid @RequestBody RestablecerPasswordRequestDto dto, HttpServletRequest request) {
        restablecerPasswordUseCase.ejecutar(dto.token(), dto.nuevaPassword(), AutenticacionActual.ipOrigen(request));
        return ResponseEntity.noContent().build();
    }

    private LoginResponseDto aResponse(LoginResultado resultado) {
        var usuario = resultado.usuario();
        return new LoginResponseDto(
                resultado.accessToken(),
                resultado.refreshToken(),
                new UsuarioResumenDto(usuario.getId(), usuario.getNombres(), usuario.getEmail(), usuario.getRol(), usuario.getEspecialidad()));
    }
}

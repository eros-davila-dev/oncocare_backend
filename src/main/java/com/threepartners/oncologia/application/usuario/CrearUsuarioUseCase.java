package com.threepartners.oncologia.application.usuario;

import com.threepartners.oncologia.domain.shared.exception.ConflictoDeNegocioException;
import com.threepartners.oncologia.domain.usuario.EstadoCuenta;
import com.threepartners.oncologia.domain.usuario.PasswordEncoderPort;
import com.threepartners.oncologia.domain.usuario.Usuario;
import com.threepartners.oncologia.domain.usuario.UsuarioRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class CrearUsuarioUseCase {

    private final UsuarioRepositoryPort usuarioRepositoryPort;
    private final PasswordEncoderPort passwordEncoderPort;

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public Usuario ejecutar(Usuario nuevoUsuario, String passwordEnTextoPlano) {
        if (usuarioRepositoryPort.existePorEmail(nuevoUsuario.getEmail())) {
            throw new ConflictoDeNegocioException("Ya existe un usuario registrado con ese correo electronico");
        }

        nuevoUsuario.setPasswordHash(passwordEncoderPort.encriptar(passwordEnTextoPlano));
        nuevoUsuario.setActivo(true);
        nuevoUsuario.setFechaCreacion(Instant.now());
        nuevoUsuario.setEstadoCuenta(EstadoCuenta.ACTIVA);
        return usuarioRepositoryPort.guardar(nuevoUsuario);
    }
}

package com.threepartners.oncologia.application.usuario;

import com.threepartners.oncologia.domain.auditoria.ResultadoAuditoria;
import com.threepartners.oncologia.domain.auditoria.event.UsuarioRolCambiadoEvent;
import com.threepartners.oncologia.domain.shared.exception.RecursoNoEncontradoException;
import com.threepartners.oncologia.domain.usuario.Rol;
import com.threepartners.oncologia.domain.usuario.Usuario;
import com.threepartners.oncologia.domain.usuario.UsuarioRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CambiarRolUsuarioUseCase {

    private final UsuarioRepositoryPort usuarioRepositoryPort;
    private final ApplicationEventPublisher eventPublisher;

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public Usuario ejecutar(Long usuarioId, Rol nuevoRol, Long usuarioEjecutorId, String ipOrigen) {
        Usuario usuario = usuarioRepositoryPort.buscarPorId(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario", usuarioId));

        Rol rolAnterior = usuario.getRol();
        usuario.setRol(nuevoRol);
        Usuario actualizado = usuarioRepositoryPort.guardar(usuario);

        eventPublisher.publishEvent(new UsuarioRolCambiadoEvent(
                usuarioEjecutorId,
                String.valueOf(usuarioId),
                rolAnterior.name(),
                nuevoRol.name(),
                ipOrigen,
                ResultadoAuditoria.EXITO));

        return actualizado;
    }
}

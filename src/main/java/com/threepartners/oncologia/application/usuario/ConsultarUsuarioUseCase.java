package com.threepartners.oncologia.application.usuario;

import com.threepartners.oncologia.domain.shared.CriterioPaginacion;
import com.threepartners.oncologia.domain.shared.Pagina;
import com.threepartners.oncologia.domain.shared.exception.RecursoNoEncontradoException;
import com.threepartners.oncologia.domain.usuario.Especialidad;
import com.threepartners.oncologia.domain.usuario.Usuario;
import com.threepartners.oncologia.domain.usuario.UsuarioRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConsultarUsuarioUseCase {

    private final UsuarioRepositoryPort usuarioRepositoryPort;

    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'RECEPCIONISTA')")
    @Transactional(readOnly = true)
    public Usuario porId(Long id) {
        return usuarioRepositoryPort.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario", id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public Pagina<Usuario> listar(CriterioPaginacion criterio) {
        return usuarioRepositoryPort.listar(criterio);
    }

    @Transactional(readOnly = true)
    public List<Usuario> porEspecialidad(Especialidad especialidad) {
        return usuarioRepositoryPort.listarPorEspecialidad(especialidad);
    }
}

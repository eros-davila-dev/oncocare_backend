package com.threepartners.oncologia.infrastructure.out.persistence.jpa;

import com.threepartners.oncologia.domain.shared.CriterioPaginacion;
import com.threepartners.oncologia.domain.shared.Pagina;
import com.threepartners.oncologia.domain.usuario.Especialidad;
import com.threepartners.oncologia.domain.usuario.Usuario;
import com.threepartners.oncologia.domain.usuario.UsuarioRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UsuarioRepositoryAdapter implements UsuarioRepositoryPort {

    private final UsuarioJpaRepository jpaRepository;

    @Override
    public Usuario guardar(Usuario usuario) {
        UsuarioJpaEntity entidad = aEntidad(usuario);
        return aDominio(jpaRepository.save(entidad));
    }

    @Override
    public Optional<Usuario> buscarPorId(Long id) {
        return jpaRepository.findById(id).map(UsuarioRepositoryAdapter::aDominio);
    }

    @Override
    public Optional<Usuario> buscarPorEmail(String email) {
        return jpaRepository.findByEmail(email).map(UsuarioRepositoryAdapter::aDominio);
    }

    @Override
    public boolean existePorEmail(String email) {
        return jpaRepository.existsByEmail(email);
    }

    @Override
    public List<Usuario> listarPorEspecialidad(Especialidad especialidad) {
        return jpaRepository.findByEspecialidadAndActivoTrue(especialidad).stream()
                .map(UsuarioRepositoryAdapter::aDominio)
                .toList();
    }

    @Override
    public Pagina<Usuario> listar(CriterioPaginacion criterio) {
        var pageable = PaginacionMapper.aPageable(criterio, "nombres");
        return PaginacionMapper.aPagina(jpaRepository.findAll(pageable), UsuarioRepositoryAdapter::aDominio);
    }

    private static UsuarioJpaEntity aEntidad(Usuario usuario) {
        return UsuarioJpaEntity.builder()
                .id(usuario.getId())
                .nombres(usuario.getNombres())
                .email(usuario.getEmail())
                .passwordHash(usuario.getPasswordHash())
                .rol(usuario.getRol())
                .especialidad(usuario.getEspecialidad())
                .activo(usuario.isActivo())
                .fechaCreacion(usuario.getFechaCreacion())
                .estadoCuenta(usuario.getEstadoCuenta())
                .intentosFallidos(usuario.getIntentosFallidos())
                .bloqueadoHasta(usuario.getBloqueadoHasta())
                .build();
    }

    private static Usuario aDominio(UsuarioJpaEntity entidad) {
        return Usuario.builder()
                .id(entidad.getId())
                .nombres(entidad.getNombres())
                .email(entidad.getEmail())
                .passwordHash(entidad.getPasswordHash())
                .rol(entidad.getRol())
                .especialidad(entidad.getEspecialidad())
                .activo(entidad.isActivo())
                .fechaCreacion(entidad.getFechaCreacion())
                .estadoCuenta(entidad.getEstadoCuenta())
                .intentosFallidos(entidad.getIntentosFallidos())
                .bloqueadoHasta(entidad.getBloqueadoHasta())
                .build();
    }
}

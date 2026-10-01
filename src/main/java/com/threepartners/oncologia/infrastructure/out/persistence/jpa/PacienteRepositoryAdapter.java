package com.threepartners.oncologia.infrastructure.out.persistence.jpa;

import com.threepartners.oncologia.domain.paciente.ConvenioSeguro;
import com.threepartners.oncologia.domain.paciente.EstadoTratamientoPaciente;
import com.threepartners.oncologia.domain.paciente.Paciente;
import com.threepartners.oncologia.domain.paciente.PacienteRepositoryPort;
import com.threepartners.oncologia.domain.paciente.PacienteResumen;
import com.threepartners.oncologia.domain.shared.CriterioPaginacion;
import com.threepartners.oncologia.domain.shared.Pagina;
import com.threepartners.oncologia.domain.usuario.Especialidad;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PacienteRepositoryAdapter implements PacienteRepositoryPort {

    private final PacienteJpaRepository jpaRepository;

    @Override
    public Paciente guardar(Paciente paciente) {
        return aDominio(jpaRepository.save(aEntidad(paciente)));
    }

    @Override
    public Optional<Paciente> buscarPorId(Long id) {
        return jpaRepository.findById(id).map(PacienteRepositoryAdapter::aDominio);
    }

    @Override
    public Optional<Paciente> buscarPorDocumento(String documentoIdentidad) {
        return jpaRepository.findByDocumentoIdentidad(documentoIdentidad).map(PacienteRepositoryAdapter::aDominio);
    }

    @Override
    public boolean existePorDocumento(String documentoIdentidad) {
        return jpaRepository.existsByDocumentoIdentidad(documentoIdentidad);
    }

    @Override
    public boolean existePorDocumentoYNoId(String documentoIdentidad, Long idExcluido) {
        return jpaRepository.existsByDocumentoIdentidadAndIdNot(documentoIdentidad, idExcluido);
    }

    @Override
    public boolean existePorEmail(String email) {
        return email != null && jpaRepository.existsByEmail(email);
    }

    @Override
    public boolean existePorEmailYNoId(String email, Long idExcluido) {
        return email != null && jpaRepository.existsByEmailAndIdNot(email, idExcluido);
    }

    @Override
    public Optional<Paciente> buscarPorUsuarioId(Long usuarioId) {
        return jpaRepository.findByUsuarioId(usuarioId).map(PacienteRepositoryAdapter::aDominio);
    }

    @Override
    public Pagina<Paciente> buscar(String textoBusqueda, CriterioPaginacion criterio) {
        var pageable = PaginacionMapper.aPageable(criterio, "apellidos");
        return PaginacionMapper.aPagina(jpaRepository.buscar(textoBusqueda, pageable), PacienteRepositoryAdapter::aDominio);
    }

    @Override
    public Pagina<PacienteResumen> buscarResumen(String textoBusqueda, EstadoTratamientoPaciente estado, CriterioPaginacion criterio) {
        var pageable = PaginacionMapper.aPageable(criterio, "apellidos");
        String estadoParam = estado != null ? estado.name() : null;
        return PaginacionMapper.aPagina(
                jpaRepository.buscarResumen(textoBusqueda, estadoParam, pageable),
                PacienteRepositoryAdapter::aResumenDominio);
    }

    private static PacienteJpaEntity aEntidad(Paciente paciente) {
        return PacienteJpaEntity.builder()
                .id(paciente.getId())
                .usuarioId(paciente.getUsuarioId())
                .nombres(paciente.getNombres())
                .apellidos(paciente.getApellidos())
                .documentoIdentidad(paciente.getDocumentoIdentidad())
                .fechaNacimiento(paciente.getFechaNacimiento())
                .telefono(paciente.getTelefono())
                .email(paciente.getEmail())
                .direccion(paciente.getDireccion())
                .tipoCancer(paciente.getTipoCancer())
                .estadioClinico(paciente.getEstadioClinico())
                .fechaDiagnostico(paciente.getFechaDiagnostico())
                .medicoTratanteId(paciente.getMedicoTratanteId())
                .convenioSeguro(paciente.getConvenioSeguro())
                .contactoEmergenciaNombre(paciente.getContactoEmergenciaNombre())
                .contactoEmergenciaTelefono(paciente.getContactoEmergenciaTelefono())
                .activo(paciente.isActivo())
                .fechaRegistro(paciente.getFechaRegistro())
                .build();
    }

    private static Paciente aDominio(PacienteJpaEntity entidad) {
        return Paciente.builder()
                .id(entidad.getId())
                .usuarioId(entidad.getUsuarioId())
                .nombres(entidad.getNombres())
                .apellidos(entidad.getApellidos())
                .documentoIdentidad(entidad.getDocumentoIdentidad())
                .fechaNacimiento(entidad.getFechaNacimiento())
                .telefono(entidad.getTelefono())
                .email(entidad.getEmail())
                .direccion(entidad.getDireccion())
                .tipoCancer(entidad.getTipoCancer())
                .estadioClinico(entidad.getEstadioClinico())
                .fechaDiagnostico(entidad.getFechaDiagnostico())
                .medicoTratanteId(entidad.getMedicoTratanteId())
                .convenioSeguro(entidad.getConvenioSeguro())
                .contactoEmergenciaNombre(entidad.getContactoEmergenciaNombre())
                .contactoEmergenciaTelefono(entidad.getContactoEmergenciaTelefono())
                .activo(entidad.isActivo())
                .fechaRegistro(entidad.getFechaRegistro())
                .build();
    }

    private static PacienteResumen aResumenDominio(PacienteResumenProjection p) {
        Paciente paciente = Paciente.builder()
                .id(p.getId())
                .nombres(p.getNombres())
                .apellidos(p.getApellidos())
                .documentoIdentidad(p.getDocumentoIdentidad())
                .fechaNacimiento(p.getFechaNacimiento())
                .telefono(p.getTelefono())
                .email(p.getEmail())
                .direccion(p.getDireccion())
                .tipoCancer(p.getTipoCancer())
                .estadioClinico(p.getEstadioClinico())
                .fechaDiagnostico(p.getFechaDiagnostico())
                .medicoTratanteId(p.getMedicoTratanteId())
                .convenioSeguro(p.getConvenioSeguro() != null ? ConvenioSeguro.valueOf(p.getConvenioSeguro()) : null)
                .contactoEmergenciaNombre(p.getContactoEmergenciaNombre())
                .contactoEmergenciaTelefono(p.getContactoEmergenciaTelefono())
                .activo(Boolean.TRUE.equals(p.getActivo()))
                .fechaRegistro(p.getFechaRegistro())
                .build();

        return new PacienteResumen(
                paciente,
                EstadoTratamientoPaciente.valueOf(p.getEstadoTratamiento()),
                p.getMedicoNombre(),
                p.getMedicoEspecialidad() != null ? Especialidad.valueOf(p.getMedicoEspecialidad()) : null,
                p.getUltimaCita(),
                p.getProximaCita());
    }
}

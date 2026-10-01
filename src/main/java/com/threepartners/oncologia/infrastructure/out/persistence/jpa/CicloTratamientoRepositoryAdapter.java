package com.threepartners.oncologia.infrastructure.out.persistence.jpa;

import com.threepartners.oncologia.domain.shared.CriterioPaginacion;
import com.threepartners.oncologia.domain.shared.Pagina;
import com.threepartners.oncologia.domain.tratamiento.CicloTratamiento;
import com.threepartners.oncologia.domain.tratamiento.CicloTratamientoRepositoryPort;
import com.threepartners.oncologia.domain.tratamiento.TipoTratamiento;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CicloTratamientoRepositoryAdapter implements CicloTratamientoRepositoryPort {

    private final CicloTratamientoJpaRepository jpaRepository;

    @Override
    public CicloTratamiento guardar(CicloTratamiento ciclo) {
        return aDominio(jpaRepository.save(aEntidad(ciclo)));
    }

    @Override
    public Optional<CicloTratamiento> buscarPorId(Long id) {
        return jpaRepository.findById(id).map(CicloTratamientoRepositoryAdapter::aDominio);
    }

    @Override
    public List<CicloTratamiento> listarPorPaciente(Long pacienteId) {
        return jpaRepository.findByPacienteIdOrderByFechaSesionAsc(pacienteId).stream()
                .map(CicloTratamientoRepositoryAdapter::aDominio)
                .toList();
    }

    @Override
    public Pagina<CicloTratamiento> listar(Long pacienteId, TipoTratamiento tipo, CriterioPaginacion criterio) {
        var pageable = PaginacionMapper.aPageable(criterio, "fechaSesion");
        return PaginacionMapper.aPagina(jpaRepository.filtrar(pacienteId, tipo, pageable), CicloTratamientoRepositoryAdapter::aDominio);
    }

    private static CicloTratamientoJpaEntity aEntidad(CicloTratamiento ciclo) {
        return CicloTratamientoJpaEntity.builder()
                .id(ciclo.getId())
                .pacienteId(ciclo.getPacienteId())
                .tipoTratamiento(ciclo.getTipoTratamiento())
                .numeroSesion(ciclo.getNumeroSesion())
                .totalSesionesEsquema(ciclo.getTotalSesionesEsquema())
                .fechaSesion(ciclo.getFechaSesion())
                .medicoResponsableId(ciclo.getMedicoResponsableId())
                .estado(ciclo.getEstado())
                .observaciones(ciclo.getObservaciones())
                .build();
    }

    private static CicloTratamiento aDominio(CicloTratamientoJpaEntity entidad) {
        return CicloTratamiento.builder()
                .id(entidad.getId())
                .pacienteId(entidad.getPacienteId())
                .tipoTratamiento(entidad.getTipoTratamiento())
                .numeroSesion(entidad.getNumeroSesion())
                .totalSesionesEsquema(entidad.getTotalSesionesEsquema())
                .fechaSesion(entidad.getFechaSesion())
                .medicoResponsableId(entidad.getMedicoResponsableId())
                .estado(entidad.getEstado())
                .observaciones(entidad.getObservaciones())
                .build();
    }

    @Override
    public Double promedioCumplimiento() {
        return jpaRepository.promedioCumplimiento();
    }
}

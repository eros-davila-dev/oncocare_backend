package com.threepartners.oncologia.infrastructure.out.persistence.jpa;

import com.threepartners.oncologia.domain.auditoria.AuditoriaAccion;
import com.threepartners.oncologia.domain.auditoria.AuditoriaRepositoryPort;
import com.threepartners.oncologia.domain.shared.CriterioPaginacion;
import com.threepartners.oncologia.domain.shared.Pagina;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class AuditoriaRepositoryAdapter implements AuditoriaRepositoryPort {

    private final AuditoriaJpaRepository jpaRepository;

    @Override
    public void registrar(AuditoriaAccion accion) {
        jpaRepository.save(aEntidad(accion));
    }

    @Override
    public Pagina<AuditoriaAccion> buscar(Long usuarioId, String entidadAfectada, Instant desde, Instant hasta, CriterioPaginacion criterio) {
        var pageable = PaginacionMapper.aPageable(criterio, "fecha");
        return PaginacionMapper.aPagina(
                jpaRepository.buscar(usuarioId, entidadAfectada, desde, hasta, pageable),
                AuditoriaRepositoryAdapter::aDominio);
    }

    private static AuditoriaJpaEntity aEntidad(AuditoriaAccion accion) {
        return AuditoriaJpaEntity.builder()
                .usuarioId(accion.getUsuarioId())
                .accion(accion.getAccion())
                .entidadAfectada(accion.getEntidadAfectada())
                .entidadId(accion.getEntidadId())
                .valoresPrevios(accion.getValoresPrevios())
                .valoresNuevos(accion.getValoresNuevos())
                .ipOrigen(accion.getIpOrigen())
                .fecha(accion.getFecha())
                .resultado(accion.getResultado())
                .build();
    }

    private static AuditoriaAccion aDominio(AuditoriaJpaEntity entidad) {
        return AuditoriaAccion.builder()
                .id(entidad.getId())
                .usuarioId(entidad.getUsuarioId())
                .accion(entidad.getAccion())
                .entidadAfectada(entidad.getEntidadAfectada())
                .entidadId(entidad.getEntidadId())
                .valoresPrevios(entidad.getValoresPrevios())
                .valoresNuevos(entidad.getValoresNuevos())
                .ipOrigen(entidad.getIpOrigen())
                .fecha(entidad.getFecha())
                .resultado(entidad.getResultado())
                .build();
    }
}

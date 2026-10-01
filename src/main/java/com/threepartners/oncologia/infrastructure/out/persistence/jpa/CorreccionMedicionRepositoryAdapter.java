package com.threepartners.oncologia.infrastructure.out.persistence.jpa;

import com.threepartners.oncologia.domain.estudio.CorreccionMedicion;
import com.threepartners.oncologia.domain.estudio.CorreccionMedicionRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CorreccionMedicionRepositoryAdapter implements CorreccionMedicionRepositoryPort {

    private final CorreccionMedicionJpaRepository jpaRepository;

    @Override
    public CorreccionMedicion guardar(CorreccionMedicion c) {
        var guardada = jpaRepository.save(CorreccionMedicionJpaEntity.builder()
                .entidad(c.entidad())
                .entidadId(c.entidadId())
                .campo(c.campo())
                .valorPrevio(c.valorPrevio())
                .valorNuevo(c.valorNuevo())
                .motivo(c.motivo())
                .usuarioId(c.usuarioId())
                .fecha(c.fecha())
                .build());
        return new CorreccionMedicion(guardada.getId(), guardada.getEntidad(), guardada.getEntidadId(),
                guardada.getCampo(), guardada.getValorPrevio(), guardada.getValorNuevo(), guardada.getMotivo(),
                guardada.getUsuarioId(), guardada.getFecha());
    }
}

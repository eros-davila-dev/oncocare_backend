package com.threepartners.oncologia.infrastructure.out.persistence.jpa;

import com.threepartners.oncologia.domain.estudio.Fase;
import com.threepartners.oncologia.domain.estudio.FaseEstudio;
import com.threepartners.oncologia.domain.estudio.FaseEstudioRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class FaseEstudioRepositoryAdapter implements FaseEstudioRepositoryPort {

    private final FaseEstudioJpaRepository jpaRepository;

    @Override
    public List<FaseEstudio> listar() {
        return jpaRepository.findAll(Sort.by("fechaInicio")).stream()
                .map(FaseEstudioRepositoryAdapter::aDominio)
                .toList();
    }

    @Override
    public Optional<FaseEstudio> buscarPorFase(Fase fase) {
        return jpaRepository.findByFase(fase).map(FaseEstudioRepositoryAdapter::aDominio);
    }

    @Override
    public FaseEstudio guardar(FaseEstudio fase) {
        var entidad = FaseEstudioJpaEntity.builder()
                .id(fase.getId())
                .fase(fase.getFase())
                .fechaInicio(fase.getFechaInicio())
                .fechaFin(fase.getFechaFin())
                .estado(fase.getEstado())
                .actualizadoEn(Instant.now())
                .build();
        return aDominio(jpaRepository.save(entidad));
    }

    private static FaseEstudio aDominio(FaseEstudioJpaEntity e) {
        return FaseEstudio.builder()
                .id(e.getId())
                .fase(e.getFase())
                .fechaInicio(e.getFechaInicio())
                .fechaFin(e.getFechaFin())
                .estado(e.getEstado())
                .build();
    }
}

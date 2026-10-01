package com.threepartners.oncologia.infrastructure.out.persistence.jpa;

import com.threepartners.oncologia.domain.estudio.Fase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FaseEstudioJpaRepository extends JpaRepository<FaseEstudioJpaEntity, Long> {

    Optional<FaseEstudioJpaEntity> findByFase(Fase fase);
}

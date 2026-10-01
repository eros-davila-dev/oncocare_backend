package com.threepartners.oncologia.infrastructure.out.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ConsultaJpaRepository extends JpaRepository<ConsultaJpaEntity, Long>,
        JpaSpecificationExecutor<ConsultaJpaEntity> {
}

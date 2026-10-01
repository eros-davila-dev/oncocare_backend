package com.threepartners.oncologia.infrastructure.out.persistence.jpa;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConversacionChatbotJpaRepository extends JpaRepository<ConversacionChatbotJpaEntity, Long> {

    List<ConversacionChatbotJpaEntity> findBySesionIdOrderByFechaDesc(String sesionId, Pageable pageable);

    List<ConversacionChatbotJpaEntity> findByConsultaIdOrderByFechaAscIdAsc(Long consultaId);
}

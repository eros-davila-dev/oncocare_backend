package com.threepartners.oncologia.infrastructure.out.persistence.jpa;

import com.threepartners.oncologia.domain.chatbot.PreguntaFrecuente;
import com.threepartners.oncologia.domain.chatbot.PreguntaFrecuenteRepositoryPort;
import com.threepartners.oncologia.config.CacheConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Las preguntas activas se leen en cada mensaje del chatbot y en cada visita
 * al portal, y casi nunca cambian: se cachean y se invalidan al guardar
 * (despues del commit, ver CacheConfig).
 */
@Component
@RequiredArgsConstructor
public class PreguntaFrecuenteRepositoryAdapter implements PreguntaFrecuenteRepositoryPort {

    private final PreguntaFrecuenteJpaRepository jpaRepository;

    @Override
    @Cacheable(CacheConfig.PREGUNTAS_FRECUENTES_ACTIVAS)
    public List<PreguntaFrecuente> listarActivas() {
        return jpaRepository.findByActivaTrueOrderByOrdenAscIdAsc().stream().map(PreguntaFrecuenteRepositoryAdapter::aDominio).toList();
    }

    @Override
    public List<PreguntaFrecuente> listarTodas() {
        return jpaRepository.findAllByOrderByOrdenAscIdAsc().stream().map(PreguntaFrecuenteRepositoryAdapter::aDominio).toList();
    }

    @Override
    public Optional<PreguntaFrecuente> buscarPorId(Long id) {
        return jpaRepository.findById(id).map(PreguntaFrecuenteRepositoryAdapter::aDominio);
    }

    @Override
    @CacheEvict(cacheNames = CacheConfig.PREGUNTAS_FRECUENTES_ACTIVAS, allEntries = true)
    public PreguntaFrecuente guardar(PreguntaFrecuente p) {
        return aDominio(jpaRepository.save(PreguntaFrecuenteJpaEntity.builder()
                .id(p.getId())
                .pregunta(p.getPregunta())
                .respuesta(p.getRespuesta())
                .categoria(p.getCategoria() != null ? p.getCategoria() : "GENERAL")
                .orden(p.getOrden())
                .activa(p.isActiva())
                .actualizadoEn(Instant.now())
                .build()));
    }

    private static PreguntaFrecuente aDominio(PreguntaFrecuenteJpaEntity e) {
        return PreguntaFrecuente.builder()
                .id(e.getId())
                .pregunta(e.getPregunta())
                .respuesta(e.getRespuesta())
                .categoria(e.getCategoria())
                .orden(e.getOrden())
                .activa(e.isActiva())
                .build();
    }
}

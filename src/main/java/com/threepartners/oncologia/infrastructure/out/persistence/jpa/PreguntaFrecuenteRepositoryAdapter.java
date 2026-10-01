package com.threepartners.oncologia.infrastructure.out.persistence.jpa;

import com.threepartners.oncologia.domain.chatbot.PreguntaFrecuente;
import com.threepartners.oncologia.domain.chatbot.PreguntaFrecuenteRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PreguntaFrecuenteRepositoryAdapter implements PreguntaFrecuenteRepositoryPort {

    private final PreguntaFrecuenteJpaRepository jpaRepository;

    @Override
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

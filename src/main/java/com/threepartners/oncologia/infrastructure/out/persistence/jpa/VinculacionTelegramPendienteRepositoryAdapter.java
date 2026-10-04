package com.threepartners.oncologia.infrastructure.out.persistence.jpa;

import com.threepartners.oncologia.domain.paciente.VinculacionTelegramPendiente;
import com.threepartners.oncologia.domain.paciente.VinculacionTelegramPendienteRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class VinculacionTelegramPendienteRepositoryAdapter implements VinculacionTelegramPendienteRepositoryPort {

    private final VinculacionTelegramPendienteJpaRepository jpaRepository;

    @Override
    public Optional<VinculacionTelegramPendiente> buscar(Long chatId) {
        return jpaRepository.findById(chatId).map(e -> VinculacionTelegramPendiente.builder()
                .chatId(e.getChatId())
                .titulares(ids(e.getTitulares()))
                .referidos(ids(e.getReferidos()))
                .intentos(e.getIntentos())
                .expiraEn(e.getExpiraEn())
                .bloqueadoHasta(e.getBloqueadoHasta())
                .build());
    }

    @Override
    public void guardar(VinculacionTelegramPendiente p) {
        jpaRepository.save(VinculacionTelegramPendienteJpaEntity.builder()
                .chatId(p.getChatId())
                .titulares(texto(p.getTitulares()))
                .referidos(texto(p.getReferidos()))
                .intentos(p.getIntentos())
                .expiraEn(p.getExpiraEn())
                .bloqueadoHasta(p.getBloqueadoHasta())
                .build());
    }

    @Override
    public void eliminar(Long chatId) {
        jpaRepository.deleteById(chatId);
    }

    private static List<Long> ids(String texto) {
        if (texto == null || texto.isBlank()) {
            return new java.util.ArrayList<>();
        }
        return Arrays.stream(texto.split(",")).map(String::strip).filter(s -> !s.isEmpty()).map(Long::valueOf)
                .collect(Collectors.toCollection(java.util.ArrayList::new));
    }

    private static String texto(List<Long> ids) {
        return ids.stream().map(String::valueOf).collect(Collectors.joining(","));
    }
}

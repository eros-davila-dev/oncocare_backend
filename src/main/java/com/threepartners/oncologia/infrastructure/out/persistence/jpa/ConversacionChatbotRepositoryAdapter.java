package com.threepartners.oncologia.infrastructure.out.persistence.jpa;

import com.threepartners.oncologia.domain.chatbot.ConversacionChatbot;
import com.threepartners.oncologia.domain.chatbot.ConversacionChatbotRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ConversacionChatbotRepositoryAdapter implements ConversacionChatbotRepositoryPort {

    private final ConversacionChatbotJpaRepository jpaRepository;

    @Override
    public ConversacionChatbot guardar(ConversacionChatbot conversacion) {
        ConversacionChatbotJpaEntity entidad = aEntidad(conversacion);
        return aDominio(jpaRepository.save(entidad));
    }

    @Override
    public List<ConversacionChatbot> listarPorConsulta(Long consultaId) {
        return jpaRepository.findByConsultaIdOrderByFechaAscIdAsc(consultaId).stream()
                .map(ConversacionChatbotRepositoryAdapter::aDominio)
                .toList();
    }

    @Override
    public List<ConversacionChatbot> listarPorSesion(String sesionId, int limite) {
        var pageable = PageRequest.of(0, limite, Sort.by(Sort.Direction.DESC, "fecha"));
        return jpaRepository.findBySesionIdOrderByFechaDesc(sesionId, pageable).stream()
                .map(ConversacionChatbotRepositoryAdapter::aDominio)
                // La consulta trae lo mas reciente primero; Gemini necesita el
                // orden cronologico real de la conversacion.
                .sorted(Comparator.comparing(ConversacionChatbot::getFecha))
                .toList();
    }

    private static ConversacionChatbotJpaEntity aEntidad(ConversacionChatbot conversacion) {
        return ConversacionChatbotJpaEntity.builder()
                .id(conversacion.getId())
                .pacienteId(conversacion.getPacienteId())
                .sesionId(conversacion.getSesionId())
                .consultaId(conversacion.getConsultaId())
                .mensajeUsuario(conversacion.getMensajeUsuario())
                .respuestaBot(conversacion.getRespuestaBot())
                .intencionDetectada(conversacion.getIntencionDetectada())
                .fecha(conversacion.getFecha())
                .canal(conversacion.getCanal())
                .build();
    }

    private static ConversacionChatbot aDominio(ConversacionChatbotJpaEntity entidad) {
        return ConversacionChatbot.builder()
                .id(entidad.getId())
                .pacienteId(entidad.getPacienteId())
                .sesionId(entidad.getSesionId())
                .consultaId(entidad.getConsultaId())
                .mensajeUsuario(entidad.getMensajeUsuario())
                .respuestaBot(entidad.getRespuestaBot())
                .intencionDetectada(entidad.getIntencionDetectada())
                .fecha(entidad.getFecha())
                .canal(entidad.getCanal())
                .build();
    }
}

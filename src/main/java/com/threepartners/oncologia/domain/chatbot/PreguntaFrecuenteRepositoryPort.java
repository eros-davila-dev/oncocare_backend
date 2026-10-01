package com.threepartners.oncologia.domain.chatbot;

import java.util.List;
import java.util.Optional;

public interface PreguntaFrecuenteRepositoryPort {

    List<PreguntaFrecuente> listarActivas();

    List<PreguntaFrecuente> listarTodas();

    Optional<PreguntaFrecuente> buscarPorId(Long id);

    PreguntaFrecuente guardar(PreguntaFrecuente pregunta);
}

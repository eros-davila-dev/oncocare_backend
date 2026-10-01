package com.threepartners.oncologia.domain.estudio;

import java.util.List;
import java.util.Optional;

public interface ParticipanteEstudioRepositoryPort {

    ParticipanteEstudio guardar(ParticipanteEstudio participante);

    Optional<ParticipanteEstudio> buscarPorId(Long id);

    Optional<ParticipanteEstudio> buscarPorPacienteId(Long pacienteId);

    Optional<ParticipanteEstudio> buscarPorCodigo(String codigo);

    boolean existePorPacienteId(Long pacienteId);

    List<ParticipanteResumen> listar();

    /** Mayor numero usado en los codigos Pnn, para asignar el siguiente. */
    int maximoNumeroCodigo();
}

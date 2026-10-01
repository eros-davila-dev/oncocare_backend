package com.threepartners.oncologia.domain.dispositivo;

import java.util.List;
import java.util.Optional;

public interface DispositivoRepositoryPort {

    DispositivoExterno guardar(DispositivoExterno dispositivo);

    Optional<DispositivoExterno> buscarPorId(Long id);

    Optional<DispositivoExterno> buscarPorCredencialHash(String credencialHash);

    List<DispositivoExterno> listarTodos();

    LecturaDispositivo guardarLectura(LecturaDispositivo lectura);

    List<LecturaDispositivo> listarLecturasPorPaciente(Long pacienteId);

    List<LecturaDispositivo> listarLecturasPorCiclo(Long cicloTratamientoId);
}

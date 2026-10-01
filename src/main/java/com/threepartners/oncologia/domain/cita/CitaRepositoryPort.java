package com.threepartners.oncologia.domain.cita;

import com.threepartners.oncologia.domain.shared.CriterioPaginacion;
import com.threepartners.oncologia.domain.shared.Pagina;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CitaRepositoryPort {

    Cita guardar(Cita cita);

    Optional<Cita> buscarPorId(Long id);

    boolean existeSolapamiento(Long medicoId, LocalDate fecha, java.time.LocalTime hora, Long idExcluido);

    Pagina<Cita> listar(Long pacienteId, Long medicoId, LocalDate desde, LocalDate hasta, EstadoCita estado, CriterioPaginacion criterio);

    List<Cita> listarProximasEnVentana(LocalDateTime desde, LocalDateTime hasta);

    /** Evita importar dos veces la misma fila de la ficha de ausentismo del pretest. */
    boolean existeCapturaPretest(Long pacienteId, LocalDate fecha, java.time.LocalTime hora);
}

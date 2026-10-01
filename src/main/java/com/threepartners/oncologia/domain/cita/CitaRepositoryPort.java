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

    /** Agenda de un dia (excluye citas del pretest transcritas), ordenada por hora. */
    List<CitaAgenda> agendaDelDia(LocalDate fecha, Long medicoId);

    /**
     * Citas cuya fecha ya paso (antes de {@code antesDe}) y que siguen sin
     * desenlace: si nadie las cierra, el TNS quedaria incompleto.
     */
    List<CitaAgenda> pendientesDeCierre(LocalDate antesDe);

    /** Citas sin desenlace cuya fecha y hora son anteriores al limite (cierre automatico). */
    List<Cita> sinDesenlaceAntesDe(LocalDateTime limite);

    /** Evita importar dos veces la misma fila de la ficha de ausentismo del pretest. */
    boolean existeCapturaPretest(Long pacienteId, LocalDate fecha, java.time.LocalTime hora);
}

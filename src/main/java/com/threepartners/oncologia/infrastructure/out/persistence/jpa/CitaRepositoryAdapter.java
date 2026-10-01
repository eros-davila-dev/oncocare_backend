package com.threepartners.oncologia.infrastructure.out.persistence.jpa;

import com.threepartners.oncologia.domain.cita.Cita;
import com.threepartners.oncologia.domain.cita.CitaAgenda;
import com.threepartners.oncologia.domain.cita.CitaRepositoryPort;
import com.threepartners.oncologia.domain.cita.EstadoCita;
import com.threepartners.oncologia.domain.cita.OrigenCita;
import com.threepartners.oncologia.domain.shared.CriterioPaginacion;
import com.threepartners.oncologia.domain.shared.Pagina;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CitaRepositoryAdapter implements CitaRepositoryPort {

    private final CitaJpaRepository jpaRepository;

    @Override
    public Cita guardar(Cita cita) {
        return aDominio(jpaRepository.save(aEntidad(cita)));
    }

    @Override
    public Optional<Cita> buscarPorId(Long id) {
        return jpaRepository.findById(id).map(CitaRepositoryAdapter::aDominio);
    }

    @Override
    public boolean existeSolapamiento(Long medicoId, LocalDate fecha, LocalTime hora, Long idExcluido) {
        return jpaRepository.existeSolapamiento(medicoId, fecha, hora, idExcluido);
    }

    @Override
    public Pagina<Cita> listar(Long pacienteId, Long medicoId, LocalDate desde, LocalDate hasta, EstadoCita estado, CriterioPaginacion criterio) {
        var pageable = PaginacionMapper.aPageable(criterio, "fecha");
        return PaginacionMapper.aPagina(
                jpaRepository.filtrar(pacienteId, medicoId, desde, hasta, estado, pageable),
                CitaRepositoryAdapter::aDominio);
    }

    @Override
    public List<Cita> listarProximasEnVentana(LocalDateTime desde, LocalDateTime hasta) {
        return jpaRepository.listarProximasEnVentana(desde, hasta).stream()
                .map(CitaRepositoryAdapter::aDominio)
                .toList();
    }

    @Override
    public boolean existeCapturaPretest(Long pacienteId, LocalDate fecha, LocalTime hora) {
        return jpaRepository.existsByPacienteIdAndFechaAndHoraAndOrigen(pacienteId, fecha,
                hora != null ? hora : LocalTime.MIDNIGHT, OrigenCita.CAPTURA_PRETEST);
    }

    @Override
    public List<CitaAgenda> agendaDelDia(LocalDate fecha, Long medicoId) {
        return jpaRepository.agendaDelDia(fecha).stream()
                .map(CitaRepositoryAdapter::aAgenda)
                .filter(c -> medicoId == null || medicoId.equals(c.cita().getMedicoId()))
                .toList();
    }

    @Override
    public List<CitaAgenda> pendientesDeCierre(LocalDate antesDe) {
        return jpaRepository.pendientesDeCierre(antesDe).stream().map(CitaRepositoryAdapter::aAgenda).toList();
    }

    @Override
    public List<Cita> sinDesenlaceAntesDe(LocalDateTime limite) {
        return jpaRepository.sinDesenlaceAntesDe(limite).stream().map(CitaRepositoryAdapter::aDominio).toList();
    }

    private static CitaAgenda aAgenda(Object[] fila) {
        return new CitaAgenda(aDominio((CitaJpaEntity) fila[0]),
                "%s %s".formatted(fila[1], fila[2]), (String) fila[3], (String) fila[4], (String) fila[5]);
    }

    private static CitaJpaEntity aEntidad(Cita cita) {
        return CitaJpaEntity.builder()
                .id(cita.getId())
                .pacienteId(cita.getPacienteId())
                .medicoId(cita.getMedicoId())
                .fecha(cita.getFecha())
                .hora(cita.getHora())
                .tipoConsulta(cita.getTipoConsulta())
                .estado(cita.getEstado())
                .observaciones(cita.getObservaciones())
                .origen(cita.getOrigen() != null ? cita.getOrigen() : OrigenCita.INTRANET)
                .fechaCreacion(cita.getFechaCreacion() != null ? cita.getFechaCreacion() : Instant.now())
                .fechaHoraDesenlace(cita.getFechaHoraDesenlace())
                .desenlaceRegistradoPor(cita.getDesenlaceRegistradoPor())
                .cierreAutomatico(cita.isCierreAutomatico())
                .vecesReprogramada(cita.getVecesReprogramada())
                .build();
    }

    private static Cita aDominio(CitaJpaEntity entidad) {
        return Cita.builder()
                .id(entidad.getId())
                .pacienteId(entidad.getPacienteId())
                .medicoId(entidad.getMedicoId())
                .fecha(entidad.getFecha())
                .hora(entidad.getHora())
                .tipoConsulta(entidad.getTipoConsulta())
                .estado(entidad.getEstado())
                .observaciones(entidad.getObservaciones())
                .origen(entidad.getOrigen())
                .fechaCreacion(entidad.getFechaCreacion())
                .fechaHoraDesenlace(entidad.getFechaHoraDesenlace())
                .desenlaceRegistradoPor(entidad.getDesenlaceRegistradoPor())
                .cierreAutomatico(entidad.isCierreAutomatico())
                .vecesReprogramada(entidad.getVecesReprogramada())
                .build();
    }
}

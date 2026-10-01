package com.threepartners.oncologia.infrastructure.out.persistence.jpa;

import com.threepartners.oncologia.domain.cita.Cita;
import com.threepartners.oncologia.domain.cita.CitaRepositoryPort;
import com.threepartners.oncologia.domain.cita.EstadoCita;
import com.threepartners.oncologia.domain.shared.CriterioPaginacion;
import com.threepartners.oncologia.domain.shared.Pagina;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

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
                .build();
    }
}

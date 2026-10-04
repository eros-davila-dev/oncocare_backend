package com.threepartners.oncologia.infrastructure.out.persistence.jpa;

import com.threepartners.oncologia.domain.estudio.CanalConsulta;
import com.threepartners.oncologia.domain.estudio.Consulta;
import com.threepartners.oncologia.domain.estudio.ConsultaRepositoryPort;
import com.threepartners.oncologia.domain.estudio.PeriodoMedicion;
import com.threepartners.oncologia.domain.estudio.ResultadoConsulta;
import com.threepartners.oncologia.domain.shared.CriterioPaginacion;
import com.threepartners.oncologia.domain.shared.Pagina;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ConsultaRepositoryAdapter implements ConsultaRepositoryPort {

    private final ConsultaJpaRepository jpaRepository;

    @Override
    public Consulta guardar(Consulta c) {
        var entidad = ConsultaJpaEntity.builder()
                .id(c.getId())
                .canal(c.getCanal())
                .sesionId(c.getSesionId())
                .pacienteId(c.getPacienteId())
                .intencion(c.getIntencion())
                .resumen(c.getResumen())
                .resultado(c.getResultado())
                .abiertaEn(c.getAbiertaEn())
                .cerradaEn(c.getCerradaEn())
                .tiempoPrimeraRespuestaMs(c.getTiempoPrimeraRespuestaMs())
                .valoracion(c.getValoracion() != null ? c.getValoracion().shortValue() : null)
                .resueltaPorUsuarioId(c.getResueltaPorUsuarioId())
                .capturadoPor(c.getCapturadoPor())
                .observacion(c.getObservacion())
                .turnos(c.getTurnos())
                .ultimaActividadEn(c.getUltimaActividadEn() != null ? c.getUltimaActividadEn() : c.getAbiertaEn())
                .notaResolucion(c.getNotaResolucion())
                .categoria(c.getCategoria())
                .derivada(c.isDerivada())
                .reabierta(c.isReabierta())
                .build();
        return aDominio(jpaRepository.save(entidad));
    }

    @Override
    public Optional<Consulta> buscarPorId(Long id) {
        return jpaRepository.findById(id).map(ConsultaRepositoryAdapter::aDominio);
    }

    @Override
    public Optional<Consulta> buscarUltimaPorSesion(String sesionId) {
        return jpaRepository.findFirstBySesionIdOrderByAbiertaEnDescIdDesc(sesionId).map(ConsultaRepositoryAdapter::aDominio);
    }

    @Override
    public Pagina<Consulta> listarEscaladas(CriterioPaginacion criterio) {
        var pageable = PaginacionMapper.aPageable(
                new CriterioPaginacion(criterio.numeroPagina(), criterio.tamanoPagina(), "ultimaActividadEn", true), "ultimaActividadEn");
        return PaginacionMapper.aPagina(jpaRepository.findByResultado(ResultadoConsulta.ESCALADA, pageable),
                ConsultaRepositoryAdapter::aDominio);
    }

    @Override
    public Pagina<Consulta> listarPorPaciente(Long pacienteId, CriterioPaginacion criterio) {
        var pageable = PaginacionMapper.aPageable(
                new CriterioPaginacion(criterio.numeroPagina(), criterio.tamanoPagina(), "abiertaEn", false), "abiertaEn");
        return PaginacionMapper.aPagina(jpaRepository.findDelPaciente(pacienteId, pageable), ConsultaRepositoryAdapter::aDominio);
    }

    @Override
    public List<Consulta> abiertasSinActividadAntesDe(Instant limite) {
        return jpaRepository.findByResultadoIsNullAndUltimaActividadEnBefore(limite).stream()
                .map(ConsultaRepositoryAdapter::aDominio).toList();
    }

    @Override
    public List<Consulta> escaladasSinActividadAntesDe(Instant limite) {
        return jpaRepository.findByResultadoAndUltimaActividadEnBefore(ResultadoConsulta.ESCALADA, limite).stream()
                .map(ConsultaRepositoryAdapter::aDominio).toList();
    }

    @Override
    public Pagina<Consulta> listar(PeriodoMedicion periodo, CanalConsulta canal, ResultadoConsulta resultado,
                                   CriterioPaginacion criterio) {
        Instant desde = periodo != null ? periodo.inicio() : null;
        Instant hasta = periodo != null ? periodo.finExclusivo() : null;
        var pageable = PaginacionMapper.aPageable(
                new CriterioPaginacion(criterio.numeroPagina(), criterio.tamanoPagina(), "abiertaEn", false), "abiertaEn");
        return PaginacionMapper.aPagina(
                jpaRepository.findAll(FiltrosJpa.todas(
                        FiltrosJpa.desde("abiertaEn", desde),
                        FiltrosJpa.antesDe("abiertaEn", hasta),
                        FiltrosJpa.igual("canal", canal),
                        FiltrosJpa.igual("resultado", resultado)), pageable),
                ConsultaRepositoryAdapter::aDominio);
    }

    static Consulta aDominio(ConsultaJpaEntity e) {
        return Consulta.builder()
                .id(e.getId())
                .canal(e.getCanal())
                .sesionId(e.getSesionId())
                .pacienteId(e.getPacienteId())
                .intencion(e.getIntencion())
                .resumen(e.getResumen())
                .resultado(e.getResultado())
                .abiertaEn(e.getAbiertaEn())
                .cerradaEn(e.getCerradaEn())
                .tiempoPrimeraRespuestaMs(e.getTiempoPrimeraRespuestaMs())
                .valoracion(e.getValoracion() != null ? e.getValoracion().intValue() : null)
                .resueltaPorUsuarioId(e.getResueltaPorUsuarioId())
                .capturadoPor(e.getCapturadoPor())
                .observacion(e.getObservacion())
                .turnos(e.getTurnos())
                .ultimaActividadEn(e.getUltimaActividadEn())
                .notaResolucion(e.getNotaResolucion())
                .categoria(e.getCategoria())
                .derivada(e.isDerivada())
                .reabierta(e.isReabierta())
                .build();
    }
}

package com.threepartners.oncologia.infrastructure.out.persistence.jpa;

import com.threepartners.oncologia.domain.estudio.CanalMedicion;
import com.threepartners.oncologia.domain.estudio.EstadoMedicion;
import com.threepartners.oncologia.domain.estudio.MedicionRegistro;
import com.threepartners.oncologia.domain.estudio.MedicionRegistroRepositoryPort;
import com.threepartners.oncologia.domain.estudio.PeriodoMedicion;
import com.threepartners.oncologia.domain.estudio.TipoMedicion;
import com.threepartners.oncologia.domain.shared.CriterioPaginacion;
import com.threepartners.oncologia.domain.shared.Pagina;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class MedicionRegistroRepositoryAdapter implements MedicionRegistroRepositoryPort {

    private final MedicionRegistroJpaRepository jpaRepository;

    @Override
    public MedicionRegistro guardar(MedicionRegistro m) {
        var entidad = MedicionRegistroJpaEntity.builder()
                .id(m.getId())
                .tipo(m.getTipo())
                .canal(m.getCanal())
                .estado(m.getEstado())
                .sospechosa(m.isSospechosa())
                .usuarioId(m.getUsuarioId())
                .pacienteId(m.getPacienteId())
                .entidadId(m.getEntidadId())
                .inicio(m.getInicio())
                .fin(m.getFin())
                .capturadoPor(m.getCapturadoPor())
                .observacion(m.getObservacion())
                .build();
        return aDominio(jpaRepository.save(entidad));
    }

    @Override
    public Optional<MedicionRegistro> buscarPorId(Long id) {
        return jpaRepository.findById(id).map(MedicionRegistroRepositoryAdapter::aDominio);
    }

    @Override
    public int marcarAbandonadasIniciadasAntesDe(Instant limite) {
        return jpaRepository.marcarAbandonadasIniciadasAntesDe(limite);
    }

    @Override
    public boolean existeManual(Long pacienteId, TipoMedicion tipo, Instant inicio) {
        return jpaRepository.existsByPacienteIdAndTipoAndInicioAndCanal(pacienteId, tipo, inicio, CanalMedicion.MANUAL);
    }

    @Override
    public Pagina<MedicionRegistro> listar(PeriodoMedicion periodo, TipoMedicion tipo, CanalMedicion canal,
                                           EstadoMedicion estado, CriterioPaginacion criterio) {
        Instant desde = periodo != null ? periodo.inicio() : null;
        Instant hasta = periodo != null ? periodo.finExclusivo() : null;
        var pageable = PaginacionMapper.aPageable(
                new CriterioPaginacion(criterio.numeroPagina(), criterio.tamanoPagina(), "inicio", false), "inicio");
        return PaginacionMapper.aPagina(
                jpaRepository.filtrar(desde, hasta, tipo, canal, estado, pageable),
                MedicionRegistroRepositoryAdapter::aDominio);
    }

    private static MedicionRegistro aDominio(MedicionRegistroJpaEntity e) {
        return MedicionRegistro.builder()
                .id(e.getId())
                .tipo(e.getTipo())
                .canal(e.getCanal())
                .estado(e.getEstado())
                .sospechosa(e.isSospechosa())
                .usuarioId(e.getUsuarioId())
                .pacienteId(e.getPacienteId())
                .entidadId(e.getEntidadId())
                .inicio(e.getInicio())
                .fin(e.getFin())
                .capturadoPor(e.getCapturadoPor())
                .observacion(e.getObservacion())
                .build();
    }
}

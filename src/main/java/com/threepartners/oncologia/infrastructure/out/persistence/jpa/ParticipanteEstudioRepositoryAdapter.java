package com.threepartners.oncologia.infrastructure.out.persistence.jpa;

import com.threepartners.oncologia.domain.estudio.ParticipanteEstudio;
import com.threepartners.oncologia.domain.estudio.ParticipanteEstudioRepositoryPort;
import com.threepartners.oncologia.domain.estudio.ParticipanteResumen;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ParticipanteEstudioRepositoryAdapter implements ParticipanteEstudioRepositoryPort {

    private final ParticipanteEstudioJpaRepository jpaRepository;

    @Override
    public ParticipanteEstudio guardar(ParticipanteEstudio p) {
        var entidad = ParticipanteEstudioJpaEntity.builder()
                .id(p.getId())
                .pacienteId(p.getPacienteId())
                .codigo(p.getCodigo())
                .fechaConsentimiento(p.getFechaConsentimiento())
                .incluido(p.isIncluido())
                .motivoExclusion(p.getMotivoExclusion())
                .observacion(p.getObservacion())
                .fechaInclusion(p.getFechaInclusion())
                .fechaExclusion(p.getFechaExclusion())
                .build();
        return aDominio(jpaRepository.save(entidad));
    }

    @Override
    public Optional<ParticipanteEstudio> buscarPorId(Long id) {
        return jpaRepository.findById(id).map(ParticipanteEstudioRepositoryAdapter::aDominio);
    }

    @Override
    public Optional<ParticipanteEstudio> buscarPorPacienteId(Long pacienteId) {
        return jpaRepository.findByPacienteId(pacienteId).map(ParticipanteEstudioRepositoryAdapter::aDominio);
    }

    @Override
    public Optional<ParticipanteEstudio> buscarPorCodigo(String codigo) {
        return jpaRepository.findByCodigoIgnoreCase(codigo).map(ParticipanteEstudioRepositoryAdapter::aDominio);
    }

    @Override
    public boolean existePorPacienteId(Long pacienteId) {
        return jpaRepository.existsByPacienteId(pacienteId);
    }

    @Override
    public List<ParticipanteResumen> listar() {
        return jpaRepository.listarConPaciente().stream()
                .map(fila -> new ParticipanteResumen(
                        aDominio((ParticipanteEstudioJpaEntity) fila[0]),
                        "%s %s".formatted(fila[1], fila[2]),
                        (String) fila[3]))
                .toList();
    }

    @Override
    public int maximoNumeroCodigo() {
        return jpaRepository.maximoNumeroCodigo();
    }

    private static ParticipanteEstudio aDominio(ParticipanteEstudioJpaEntity e) {
        return ParticipanteEstudio.builder()
                .id(e.getId())
                .pacienteId(e.getPacienteId())
                .codigo(e.getCodigo())
                .fechaConsentimiento(e.getFechaConsentimiento())
                .incluido(e.isIncluido())
                .motivoExclusion(e.getMotivoExclusion())
                .observacion(e.getObservacion())
                .fechaInclusion(e.getFechaInclusion())
                .fechaExclusion(e.getFechaExclusion())
                .build();
    }
}

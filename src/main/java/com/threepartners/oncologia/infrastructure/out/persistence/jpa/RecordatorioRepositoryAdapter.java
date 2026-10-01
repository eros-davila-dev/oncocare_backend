package com.threepartners.oncologia.infrastructure.out.persistence.jpa;

import com.threepartners.oncologia.domain.recordatorio.CanalRecordatorio;
import com.threepartners.oncologia.domain.recordatorio.EstadoRecordatorio;
import com.threepartners.oncologia.domain.recordatorio.Recordatorio;
import com.threepartners.oncologia.domain.recordatorio.RecordatorioRepositoryPort;
import com.threepartners.oncologia.domain.recordatorio.TipoRecordatorio;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RecordatorioRepositoryAdapter implements RecordatorioRepositoryPort {

    private final RecordatorioJpaRepository jpaRepository;

    @Override
    public Recordatorio guardar(Recordatorio r) {
        var existente = r.getId() != null ? jpaRepository.findById(r.getId()).orElse(null) : null;
        return aDominio(jpaRepository.save(RecordatorioJpaEntity.builder()
                .id(r.getId())
                .citaId(r.getCitaId())
                .tipo(r.getTipo())
                .canal(r.getCanal())
                .programadoPara(r.getProgramadoPara())
                .estado(r.getEstado())
                .intentos(r.getIntentos())
                .mensajeExternoId(r.getMensajeExternoId())
                .tomadoEn(r.getTomadoEn())
                .enviadoEn(r.getEnviadoEn())
                .respuesta(r.getRespuesta())
                .respondidoEn(r.getRespondidoEn())
                .error(r.getError())
                .creadoEn(existente != null ? existente.getCreadoEn() : Instant.now())
                .build()));
    }

    @Override
    public Optional<Recordatorio> buscarPorId(Long id) {
        return jpaRepository.findById(id).map(RecordatorioRepositoryAdapter::aDominio);
    }

    @Override
    public boolean existe(Long citaId, TipoRecordatorio tipo, CanalRecordatorio canal) {
        return jpaRepository.existsByCitaIdAndTipoAndCanal(citaId, tipo, canal);
    }

    @Override
    public List<Recordatorio> bloquearPendientesVencidos(CanalRecordatorio canal, Instant ahora, int limite) {
        return jpaRepository.bloquearPendientesVencidos(canal.name(), ahora, limite).stream()
                .map(RecordatorioRepositoryAdapter::aDominio).toList();
    }

    @Override
    public List<Recordatorio> listarPendientesVencidos(CanalRecordatorio canal, Instant ahora) {
        return jpaRepository.findByEstadoAndCanalAndProgramadoParaLessThanEqualOrderByProgramadoPara(
                EstadoRecordatorio.PENDIENTE, canal, ahora).stream().map(RecordatorioRepositoryAdapter::aDominio).toList();
    }

    @Override
    public List<Recordatorio> enProcesoTomadosAntesDe(Instant limite) {
        return jpaRepository.findByEstadoAndTomadoEnBefore(EstadoRecordatorio.EN_PROCESO, limite).stream()
                .map(RecordatorioRepositoryAdapter::aDominio).toList();
    }

    @Override
    public List<Recordatorio> listarPorCita(Long citaId) {
        return jpaRepository.findByCitaIdOrderByProgramadoPara(citaId).stream()
                .map(RecordatorioRepositoryAdapter::aDominio).toList();
    }

    private static Recordatorio aDominio(RecordatorioJpaEntity e) {
        return Recordatorio.builder()
                .id(e.getId())
                .citaId(e.getCitaId())
                .tipo(e.getTipo())
                .canal(e.getCanal())
                .programadoPara(e.getProgramadoPara())
                .estado(e.getEstado())
                .intentos(e.getIntentos())
                .mensajeExternoId(e.getMensajeExternoId())
                .tomadoEn(e.getTomadoEn())
                .enviadoEn(e.getEnviadoEn())
                .respuesta(e.getRespuesta())
                .respondidoEn(e.getRespondidoEn())
                .error(e.getError())
                .build();
    }
}

package com.threepartners.oncologia.infrastructure.out.persistence.jpa;

import com.threepartners.oncologia.domain.dispositivo.DispositivoExterno;
import com.threepartners.oncologia.domain.dispositivo.DispositivoRepositoryPort;
import com.threepartners.oncologia.domain.dispositivo.LecturaDispositivo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DispositivoRepositoryAdapter implements DispositivoRepositoryPort {

    private final DispositivoExternoJpaRepository dispositivoJpaRepository;
    private final LecturaDispositivoJpaRepository lecturaJpaRepository;

    @Override
    public DispositivoExterno guardar(DispositivoExterno dispositivo) {
        return aDominio(dispositivoJpaRepository.save(aEntidad(dispositivo)));
    }

    @Override
    public Optional<DispositivoExterno> buscarPorId(Long id) {
        return dispositivoJpaRepository.findById(id).map(DispositivoRepositoryAdapter::aDominio);
    }

    @Override
    public Optional<DispositivoExterno> buscarPorCredencialHash(String credencialHash) {
        return dispositivoJpaRepository.findByCredencialHash(credencialHash).map(DispositivoRepositoryAdapter::aDominio);
    }

    @Override
    public List<DispositivoExterno> listarTodos() {
        return dispositivoJpaRepository.findAll().stream().map(DispositivoRepositoryAdapter::aDominio).toList();
    }

    @Override
    public LecturaDispositivo guardarLectura(LecturaDispositivo lectura) {
        return aDominioLectura(lecturaJpaRepository.save(aEntidadLectura(lectura)));
    }

    @Override
    public List<LecturaDispositivo> listarLecturasPorPaciente(Long pacienteId) {
        return lecturaJpaRepository.findByPacienteIdOrderByFechaLecturaDesc(pacienteId).stream()
                .map(DispositivoRepositoryAdapter::aDominioLectura)
                .toList();
    }

    @Override
    public List<LecturaDispositivo> listarLecturasPorCiclo(Long cicloTratamientoId) {
        return lecturaJpaRepository.findByCicloTratamientoIdOrderByFechaLecturaDesc(cicloTratamientoId).stream()
                .map(DispositivoRepositoryAdapter::aDominioLectura)
                .toList();
    }

    private static DispositivoExternoJpaEntity aEntidad(DispositivoExterno dispositivo) {
        return DispositivoExternoJpaEntity.builder()
                .id(dispositivo.getId())
                .nombre(dispositivo.getNombre())
                .tipo(dispositivo.getTipo())
                .protocolo(dispositivo.getProtocolo())
                .estadoConexion(dispositivo.getEstadoConexion())
                .credencialHash(dispositivo.getCredencialHash())
                .build();
    }

    private static DispositivoExterno aDominio(DispositivoExternoJpaEntity entidad) {
        return DispositivoExterno.builder()
                .id(entidad.getId())
                .nombre(entidad.getNombre())
                .tipo(entidad.getTipo())
                .protocolo(entidad.getProtocolo())
                .estadoConexion(entidad.getEstadoConexion())
                .credencialHash(entidad.getCredencialHash())
                .build();
    }

    private static LecturaDispositivoJpaEntity aEntidadLectura(LecturaDispositivo lectura) {
        return LecturaDispositivoJpaEntity.builder()
                .id(lectura.getId())
                .dispositivoId(lectura.getDispositivoId())
                .pacienteId(lectura.getPacienteId())
                .cicloTratamientoId(lectura.getCicloTratamientoId())
                .tipoDato(lectura.getTipoDato())
                .valor(lectura.getValor())
                .fechaLectura(lectura.getFechaLectura())
                .build();
    }

    private static LecturaDispositivo aDominioLectura(LecturaDispositivoJpaEntity entidad) {
        return LecturaDispositivo.builder()
                .id(entidad.getId())
                .dispositivoId(entidad.getDispositivoId())
                .pacienteId(entidad.getPacienteId())
                .cicloTratamientoId(entidad.getCicloTratamientoId())
                .tipoDato(entidad.getTipoDato())
                .valor(entidad.getValor())
                .fechaLectura(entidad.getFechaLectura())
                .build();
    }
}

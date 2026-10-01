package com.threepartners.oncologia.application.dispositivo;

import com.threepartners.oncologia.domain.dispositivo.DispositivoExterno;
import com.threepartners.oncologia.domain.dispositivo.DispositivoRepositoryPort;
import com.threepartners.oncologia.domain.dispositivo.EstadoConexion;
import com.threepartners.oncologia.domain.shared.exception.RecursoNoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GestionarDispositivoUseCase {

    private final DispositivoRepositoryPort dispositivoRepositoryPort;

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public DispositivoExterno registrar(DispositivoExterno dispositivo) {
        dispositivo.setEstadoConexion(EstadoConexion.DESCONECTADO);
        return dispositivoRepositoryPort.guardar(dispositivo);
    }

    @Transactional(readOnly = true)
    public List<DispositivoExterno> listar() {
        return dispositivoRepositoryPort.listarTodos();
    }

    @Transactional(readOnly = true)
    public List<com.threepartners.oncologia.domain.dispositivo.LecturaDispositivo> lecturasPorPaciente(Long pacienteId) {
        return dispositivoRepositoryPort.listarLecturasPorPaciente(pacienteId);
    }

    @Transactional(readOnly = true)
    public DispositivoExterno porId(Long id) {
        return dispositivoRepositoryPort.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("DispositivoExterno", id));
    }
}

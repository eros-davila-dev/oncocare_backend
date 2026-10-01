package com.threepartners.oncologia.application.dispositivo;

import com.threepartners.oncologia.domain.dispositivo.DispositivoLecturaPort;
import com.threepartners.oncologia.domain.dispositivo.DispositivoRepositoryPort;
import com.threepartners.oncologia.domain.dispositivo.LecturaDispositivo;
import com.threepartners.oncologia.domain.shared.exception.RecursoNoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Implementacion unica del puerto generico DispositivoLecturaPort (seccion 14).
 * Todos los adaptadores de entrada (MQTT, webhook REST, puente serial) convergen
 * aqui sin que este caso de uso conozca el protocolo de origen.
 */
@Service
@RequiredArgsConstructor
public class RegistrarLecturaDispositivoUseCase implements DispositivoLecturaPort {

    private final DispositivoRepositoryPort dispositivoRepositoryPort;

    @Override
    @Transactional
    public LecturaDispositivo registrarLectura(LecturaDispositivo lectura) {
        dispositivoRepositoryPort.buscarPorId(lectura.getDispositivoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("DispositivoExterno", lectura.getDispositivoId()));

        if (lectura.getFechaLectura() == null) {
            lectura.setFechaLectura(Instant.now());
        }

        return dispositivoRepositoryPort.guardarLectura(lectura);
    }
}

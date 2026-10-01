package com.threepartners.oncologia.application.estudio;

import com.threepartners.oncologia.domain.estudio.MedicionRegistroRepositoryPort;
import com.threepartners.oncologia.domain.estudio.TipoMedicion;
import com.threepartners.oncologia.domain.shared.exception.DomainException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;

/**
 * Colaborador interno de los casos de uso que guardan una cita o un paciente:
 * cierra la sesion de medicion del TPR dentro de SU transaccion, de modo que
 * el fin queda sellado en el mismo instante en que el registro existe.
 *
 * Deliberadamente no bloquea la operacion de negocio: si la sesion es
 * invalida (vencida, ajena, de otro tipo) el registro del paciente se guarda
 * igual y la medicion simplemente no cuenta. Perder un dato del indicador es
 * preferible a impedir que se atienda a un paciente.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CerrarMedicionRegistroService {

    private final MedicionRegistroRepositoryPort medicionRegistroRepositoryPort;
    private final Clock clock;

    public void cerrar(Long medicionId, TipoMedicion tipo, Long usuarioId, Long pacienteId, Long entidadId) {
        if (medicionId == null) {
            log.debug("Registro {} de la entidad {} guardado sin sesion de medicion", tipo, entidadId);
            return;
        }
        medicionRegistroRepositoryPort.buscarPorId(medicionId).ifPresentOrElse(medicion -> {
            try {
                medicion.completar(usuarioId, tipo, pacienteId, entidadId, clock.instant());
                medicionRegistroRepositoryPort.guardar(medicion);
            } catch (DomainException e) {
                log.warn("La sesion de medicion {} no se contabiliza: {}", medicionId, e.getMessage());
            }
        }, () -> log.warn("La sesion de medicion {} no existe; el registro {} no se contabiliza", medicionId, entidadId));
    }
}

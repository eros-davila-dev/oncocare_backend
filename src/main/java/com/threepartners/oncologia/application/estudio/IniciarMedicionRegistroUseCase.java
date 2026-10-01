package com.threepartners.oncologia.application.estudio;

import com.threepartners.oncologia.domain.estudio.CanalMedicion;
import com.threepartners.oncologia.domain.estudio.MedicionRegistro;
import com.threepartners.oncologia.domain.estudio.MedicionRegistroRepositoryPort;
import com.threepartners.oncologia.domain.estudio.TipoMedicion;
import com.threepartners.oncologia.domain.shared.exception.ValidacionDeNegocioException;
import com.threepartners.oncologia.domain.usuario.Rol;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * Abre una sesion de medicion del TPR cuando se muestra un formulario de
 * registro. El servidor sella el inicio; el canal tambien lo decide el
 * servidor segun el rol (el cliente no puede declarar que es "INTRANET" si es
 * un paciente), para que el filtro por canal del indicador sea confiable.
 */
@Service
@RequiredArgsConstructor
public class IniciarMedicionRegistroUseCase {

    private final MedicionRegistroRepositoryPort medicionRegistroRepositoryPort;
    private final Clock clock;

    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA', 'MEDICO', 'PACIENTE')")
    @Transactional
    public MedicionRegistro ejecutar(TipoMedicion tipo, Long usuarioId, Rol rol) {
        if (tipo == null) {
            throw new ValidacionDeNegocioException("Debe indicar el tipo de registro a medir");
        }
        CanalMedicion canal = rol == Rol.PACIENTE ? CanalMedicion.PORTAL : CanalMedicion.INTRANET;
        if (canal == CanalMedicion.PORTAL && tipo == TipoMedicion.ACTUALIZACION_PACIENTE) {
            throw new ValidacionDeNegocioException("El portal del paciente no mide actualizaciones de ficha");
        }
        return medicionRegistroRepositoryPort.guardar(
                MedicionRegistro.iniciar(tipo, canal, usuarioId, clock.instant()));
    }
}

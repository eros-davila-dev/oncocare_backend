package com.threepartners.oncologia.application.paciente;

import com.threepartners.oncologia.application.estudio.CerrarMedicionRegistroService;
import com.threepartners.oncologia.domain.auditoria.ResultadoAuditoria;
import com.threepartners.oncologia.domain.auditoria.event.PacienteRegistradoEvent;
import com.threepartners.oncologia.domain.estudio.TipoMedicion;
import com.threepartners.oncologia.domain.paciente.Paciente;
import com.threepartners.oncologia.domain.paciente.PacienteRepositoryPort;
import com.threepartners.oncologia.domain.shared.exception.ConflictoDeNegocioException;
import com.threepartners.oncologia.domain.shared.exception.RecursoNoEncontradoException;
import com.threepartners.oncologia.domain.usuario.Usuario;
import com.threepartners.oncologia.domain.usuario.UsuarioRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * Paso 2 del flujo de autoservicio (seccion 7), ejecutado por el propio
 * paciente ya autenticado (cuenta verificada): crea el registro clinico
 * (Paciente) vinculado a su cuenta. El tiempo de llenado del formulario se
 * mide con la sesion de medicion (canal PORTAL) que el propio portal abre al
 * mostrarlo; se reporta separado del registro asistido por el personal.
 */
@Service
@RequiredArgsConstructor
public class CompletarPerfilPacienteUseCase {

    private final PacienteRepositoryPort pacienteRepositoryPort;
    private final UsuarioRepositoryPort usuarioRepositoryPort;
    private final ApplicationEventPublisher eventPublisher;
    private final CerrarMedicionRegistroService cerrarMedicionRegistroService;
    private final Clock clock;

    @PreAuthorize("hasRole('PACIENTE')")
    @Transactional
    public Paciente ejecutar(Long usuarioAutenticadoId, Paciente datos, Long medicionId, String ipOrigen) {
        Usuario cuenta = usuarioRepositoryPort.buscarPorId(usuarioAutenticadoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario", usuarioAutenticadoId));

        if (pacienteRepositoryPort.buscarPorUsuarioId(usuarioAutenticadoId).isPresent()) {
            throw new ConflictoDeNegocioException("Ya completaste tu perfil de paciente");
        }
        if (pacienteRepositoryPort.existePorDocumento(datos.getDocumentoIdentidad())) {
            throw new ConflictoDeNegocioException(
                    "Ya existe un paciente registrado con el documento de identidad: " + datos.getDocumentoIdentidad());
        }
        String email = datos.getEmail() != null && !datos.getEmail().isBlank() ? datos.getEmail() : cuenta.getEmail();
        if (pacienteRepositoryPort.existePorEmail(email)) {
            throw new ConflictoDeNegocioException("Ya existe un paciente registrado con ese correo electronico");
        }

        datos.setUsuarioId(usuarioAutenticadoId);
        datos.setEmail(email);
        datos.setActivo(true);
        datos.setFechaRegistro(clock.instant());

        Paciente guardado = pacienteRepositoryPort.guardar(datos);
        cerrarMedicionRegistroService.cerrar(medicionId, TipoMedicion.REGISTRO_PACIENTE, usuarioAutenticadoId,
                guardado.getId(), guardado.getId());

        eventPublisher.publishEvent(new PacienteRegistradoEvent(
                usuarioAutenticadoId,
                String.valueOf(guardado.getId()),
                "documento=%s;autoservicio=true".formatted(guardado.getDocumentoIdentidad()),
                ipOrigen,
                ResultadoAuditoria.EXITO));

        return guardado;
    }
}

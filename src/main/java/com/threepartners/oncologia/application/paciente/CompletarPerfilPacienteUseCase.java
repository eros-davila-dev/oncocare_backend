package com.threepartners.oncologia.application.paciente;

import com.threepartners.oncologia.domain.auditoria.ResultadoAuditoria;
import com.threepartners.oncologia.domain.auditoria.event.PacienteRegistradoEvent;
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

import java.time.Duration;
import java.time.Instant;

/**
 * Paso 2 del flujo de autoservicio (seccion 7), ejecutado por el propio
 * paciente ya autenticado (cuenta verificada): crea el registro clinico
 * (Paciente) vinculado a su cuenta. tiempoRegistroSegundos mide el flujo
 * completo (creacion de cuenta -> perfil completado), a diferencia del
 * registro asistido por staff, que solo mide el tiempo de llenado del
 * formulario (seccion 20).
 */
@Service
@RequiredArgsConstructor
public class CompletarPerfilPacienteUseCase {

    private final PacienteRepositoryPort pacienteRepositoryPort;
    private final UsuarioRepositoryPort usuarioRepositoryPort;
    private final ApplicationEventPublisher eventPublisher;

    @PreAuthorize("hasRole('PACIENTE')")
    @Transactional
    public Paciente ejecutar(Long usuarioAutenticadoId, Paciente datos, String ipOrigen) {
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
        datos.setFechaRegistro(Instant.now());
        datos.setTiempoRegistroSegundos((int) Duration.between(cuenta.getFechaCreacion(), Instant.now()).toSeconds());

        Paciente guardado = pacienteRepositoryPort.guardar(datos);

        eventPublisher.publishEvent(new PacienteRegistradoEvent(
                usuarioAutenticadoId,
                String.valueOf(guardado.getId()),
                "documento=%s;autoservicio=true".formatted(guardado.getDocumentoIdentidad()),
                ipOrigen,
                ResultadoAuditoria.EXITO));

        return guardado;
    }
}

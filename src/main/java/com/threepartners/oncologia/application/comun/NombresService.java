package com.threepartners.oncologia.application.comun;

import com.threepartners.oncologia.domain.cita.Cita;
import com.threepartners.oncologia.domain.cita.CitaRepositoryPort;
import com.threepartners.oncologia.domain.estudio.ConsultaRepositoryPort;
import com.threepartners.oncologia.domain.paciente.Paciente;
import com.threepartners.oncologia.domain.paciente.PacienteRepositoryPort;
import com.threepartners.oncologia.domain.tratamiento.CicloTratamientoRepositoryPort;
import com.threepartners.oncologia.domain.usuario.Usuario;
import com.threepartners.oncologia.domain.usuario.UsuarioRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Nombres legibles para las pantallas del personal: un listado muestra
 * "Jasmin Arnao Fretel" o "Dra. Ruiz", nunca "Paciente #2" o "Medico #3".
 *
 * Solo se usa desde endpoints que ya autorizaron al usuario (citas,
 * tratamientos, auditoria, ficha del paciente): no amplia lo que puede ver,
 * solo traduce ids que ya recibe. Se consulta por pagina (20 filas), asi que
 * buscar por id es suficiente.
 */
@Service
@RequiredArgsConstructor
public class NombresService {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final PacienteRepositoryPort pacienteRepositoryPort;
    private final UsuarioRepositoryPort usuarioRepositoryPort;
    private final CitaRepositoryPort citaRepositoryPort;
    private final ConsultaRepositoryPort consultaRepositoryPort;
    private final CicloTratamientoRepositoryPort cicloRepositoryPort;

    @Transactional(readOnly = true)
    public Map<Long, String> pacientes(Collection<Long> ids) {
        Map<Long, String> nombres = new HashMap<>();
        ids.stream().filter(Objects::nonNull).distinct().forEach(id ->
                pacienteRepositoryPort.buscarPorId(id).ifPresent(p -> nombres.put(id, p.nombreCompleto())));
        return nombres;
    }

    @Transactional(readOnly = true)
    public Map<Long, String> usuarios(Collection<Long> ids) {
        Map<Long, String> nombres = new HashMap<>();
        ids.stream().filter(Objects::nonNull).distinct().forEach(id ->
                usuarioRepositoryPort.buscarPorId(id).ifPresent(u -> nombres.put(id, u.getNombres())));
        return nombres;
    }

    /**
     * Descripcion de la entidad de un registro de auditoria ("Jasmin Arnao
     * Fretel", "Cita del 03/10/2026 de Jasmin Arnao Fretel"). null si el tipo
     * no tiene una descripcion mejor que su nombre.
     */
    @Transactional(readOnly = true)
    public String entidad(String tipo, String id) {
        if (tipo == null || id == null) {
            return null;
        }
        if (!id.matches("\\d+")) {
            // Los inicios de sesion guardan el correo usado: ya es legible.
            return "USUARIO".equals(tipo) ? id : null;
        }
        Long clave = Long.valueOf(id);
        return switch (tipo) {
            case "PACIENTE" -> pacienteRepositoryPort.buscarPorId(clave).map(Paciente::nombreCompleto).orElse(null);
            case "USUARIO" -> usuarioRepositoryPort.buscarPorId(clave).map(Usuario::getNombres).orElse(null);
            case "CITA" -> citaRepositoryPort.buscarPorId(clave).map(this::describirCita).orElse(null);
            case "CONSULTA" -> consultaRepositoryPort.buscarPorId(clave)
                    .map(c -> "Consulta de " + nombrePaciente(c.getPacienteId()).orElse("un visitante sin identificar"))
                    .orElse(null);
            case "CICLO_TRATAMIENTO" -> cicloRepositoryPort.buscarPorId(clave)
                    .map(c -> "Tratamiento de " + nombrePaciente(c.getPacienteId()).orElse("paciente"))
                    .orElse(null);
            default -> null;
        };
    }

    private String describirCita(Cita cita) {
        return "Cita del " + cita.getFecha().format(FECHA) + " de " + nombrePaciente(cita.getPacienteId()).orElse("paciente");
    }

    private Optional<String> nombrePaciente(Long pacienteId) {
        return pacienteId == null ? Optional.empty() : pacienteRepositoryPort.buscarPorId(pacienteId).map(Paciente::nombreCompleto);
    }
}

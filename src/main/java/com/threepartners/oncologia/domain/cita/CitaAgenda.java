package com.threepartners.oncologia.domain.cita;

/**
 * Cita con los datos minimos para que recepcion la identifique en la agenda
 * del dia sin abrir la ficha del paciente.
 */
public record CitaAgenda(
        Cita cita,
        String pacienteNombre,
        String pacienteDocumento,
        String pacienteTelefono,
        String medicoNombre
) {
}

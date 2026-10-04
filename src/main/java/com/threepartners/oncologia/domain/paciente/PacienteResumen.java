package com.threepartners.oncologia.domain.paciente;

import com.threepartners.oncologia.domain.usuario.Especialidad;

import java.time.LocalDate;

/**
 * Proyeccion de lectura de Paciente enriquecida con datos que no viven en la
 * tabla paciente: estado de tratamiento derivado, medico tratante resuelto y
 * ultima/proxima cita. Se usa solo para el listado enriquecido (seccion UI de
 * pacientes); el CRUD normal sigue operando sobre Paciente.
 */
public record PacienteResumen(
        Paciente paciente,
        EstadoTratamientoPaciente estadoTratamiento,
        String medicoTratanteNombre,
        Especialidad medicoTratanteEspecialidad,
        LocalDate ultimaCita,
        LocalDate proximaCita,
        /** El paciente vinculo su Telegram (recibe recordatorios). */
        boolean tieneTelegram,
        /** Su referido vinculo su Telegram. */
        boolean referidoTieneTelegram
) {
}

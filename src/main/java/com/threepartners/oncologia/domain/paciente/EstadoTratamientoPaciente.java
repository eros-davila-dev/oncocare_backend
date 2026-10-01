package com.threepartners.oncologia.domain.paciente;

/**
 * Estado de tratamiento derivado a partir de los ciclos de tratamiento del
 * paciente (no es un campo propio de Paciente): se calcula segun el ciclo mas
 * reciente por fecha de sesion.
 */
public enum EstadoTratamientoPaciente {
    PENDIENTE,
    EN_TRATAMIENTO,
    FINALIZADO,
    SUSPENDIDO
}

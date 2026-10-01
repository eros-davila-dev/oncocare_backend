package com.threepartners.oncologia.domain.paciente;

/**
 * Indicadores agregados para la pantalla de pacientes. Las variaciones son
 * null cuando no hay periodo anterior contra el cual comparar (sin datos
 * suficientes), para que el frontend pueda ocultar el badge en vez de
 * inventar un porcentaje.
 */
public record EstadisticasPacientes(
        long pacientesRegistrados,
        Double variacionPacientesRegistradosPorcentaje,
        long pacientesEnTratamiento,
        Double variacionPacientesEnTratamientoPorcentaje,
        long citasEstaSemana,
        Double variacionCitasPorcentaje,
        double asistenciaCitasPorcentaje,
        Double variacionAsistenciaPorcentaje
) {
}

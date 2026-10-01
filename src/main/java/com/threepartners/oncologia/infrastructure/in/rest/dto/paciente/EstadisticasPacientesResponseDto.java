package com.threepartners.oncologia.infrastructure.in.rest.dto.paciente;

public record EstadisticasPacientesResponseDto(
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

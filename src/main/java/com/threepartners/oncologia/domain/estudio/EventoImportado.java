package com.threepartners.oncologia.domain.estudio;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Un evento por paciente del instrumento (registro, cita o consulta), con el
 * paciente identificado solo por su codigo anonimo. Los campos que no aplican
 * al tipo van en null.
 */
public record EventoImportado(Fase fase, TipoEventoImportado tipo, int numero, LocalDate fecha, String codigoPaciente,
                              LocalTime horaInicio, LocalTime horaFin, Double minutos, String modalidad,
                              String estado, String motivo, Boolean recordatorio,
                              String categoria, String canal, Boolean resuelta, Boolean derivada, Double tiempoRespuesta,
                              String controlCalidad, String fuente) {
}

package com.threepartners.oncologia.domain.estudio;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Filas individuales detras de cada indicador (las tres fichas del Anexo 2),
 * para exportarlas. Solo llevan el codigo de participante (null si el dato es
 * de alguien fuera de la muestra), nunca nombre, documento ni texto libre:
 * es lo minimo que necesita el analisis (Ley 29733). Fechas y horas en Lima.
 */
public final class RegistrosFichas {

    private RegistrosFichas() {
    }

    /** Instrumento 01 (TPR). */
    public record Tiempo(String codigo, LocalDate fecha, LocalTime horaInicio, LocalTime horaFin,
                         long segundos, TipoMedicion tipo, CanalMedicion canal) {
    }

    /** Instrumento 02 (TNS): solo citas con desenlace. */
    public record Asistencia(String codigo, LocalDate fecha, LocalTime hora, boolean asistio, String origen) {
    }

    /** Instrumento 03 (NCA): solo consultas con resultado final. */
    public record ConsultaCerrada(String codigo, LocalDate fecha, LocalTime hora, CanalConsulta canal,
                                  ResultadoConsulta resultado) {
    }
}

package com.threepartners.oncologia.application.estudio;

import com.threepartners.oncologia.domain.estudio.CanalConsulta;
import com.threepartners.oncologia.domain.estudio.TipoMedicion;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Datos de las tres fichas de recoleccion del Anexo 2 de la tesis, tal como
 * las captura el investigador (formulario o importacion de hoja de calculo).
 */
public final class FichasEstudio {

    private FichasEstudio() {
    }

    public enum TipoFicha {
        /** Instrumento 01: ficha de registro de tiempos (TPR). */
        TIEMPOS,
        /** Instrumento 02: ficha de registro de ausentismo (TNS). */
        ASISTENCIAS,
        /** Instrumento 03: ficha de registro del sistema (NCA). */
        CONSULTAS
    }

    public record FichaTiempo(Long pacienteId, TipoMedicion tipo, LocalDate fecha, LocalTime horaInicio,
                              LocalTime horaFin, String observacion) {
    }

    public record FichaAsistencia(Long pacienteId, LocalDate fecha, LocalTime hora, String tipoConsulta,
                                  boolean asistio) {
    }

    public record FichaConsulta(LocalDate fecha, LocalTime hora, CanalConsulta canal, Long pacienteId,
                                String resumen, boolean resuelta, String observacion) {
    }

    public record ErrorFila(int fila, String mensaje) {
    }

    /**
     * La importacion es todo o nada: si una fila tiene errores no se guarda
     * ninguna, para que el investigador nunca quede con una ficha a medias.
     */
    public record ResultadoImportacion(TipoFicha ficha, int filasLeidas, int filasValidas,
                                       List<ErrorFila> errores, boolean aplicado) {
    }
}

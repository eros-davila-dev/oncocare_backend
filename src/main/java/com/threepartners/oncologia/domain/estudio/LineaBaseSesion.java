package com.threepartners.oncologia.domain.estudio;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Valor de una sesion de la linea base (pretest recolectado a mano y validado
 * por la fundacion). null = esa sesion no tuvo eventos de ese indicador.
 */
public record LineaBaseSesion(Fase fase, int numero, LocalDate fecha, Double tprMin, Double taPct, Double ncaPct,
                              String fuente, Instant cargadoEn) {
}

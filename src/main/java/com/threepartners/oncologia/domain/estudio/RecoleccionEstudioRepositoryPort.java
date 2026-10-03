package com.threepartners.oncologia.domain.estudio;

/**
 * Filas evento por evento de un periodo (todos los pacientes, con codigo
 * anonimo), para la recoleccion por sesion de la tesis v8. El calculo se
 * hace en {@link RecoleccionSesiones}.
 */
public interface RecoleccionEstudioRepositoryPort {

    RecoleccionSesiones.Detalle filas(PeriodoMedicion periodo);
}

package com.threepartners.oncologia.domain.dashboard;

import com.threepartners.oncologia.domain.estudio.PeriodoMedicion;

/** Filas de actividad del periodo (con nombres) para el panel del personal. */
public interface ActividadRepositoryPort {

    ActividadPeriodo.Filas filas(PeriodoMedicion periodo);
}

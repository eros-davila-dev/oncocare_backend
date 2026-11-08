package com.threepartners.oncologia.domain.estudio;

import java.util.List;

public interface EventosImportadosRepositoryPort {

    /** Reemplaza todos los eventos importados (una carga nueva corrige la anterior). */
    void reemplazar(List<EventoImportado> eventos, Long usuarioId);

    List<EventoImportado> listar(TipoEventoImportado tipo, Fase fase);

    /** Todos los eventos importados con fecha dentro del rango (inclusive). */
    List<EventoImportado> listarEntre(java.time.LocalDate desde, java.time.LocalDate hasta);
}

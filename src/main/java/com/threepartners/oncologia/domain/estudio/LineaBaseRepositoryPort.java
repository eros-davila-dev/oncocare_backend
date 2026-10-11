package com.threepartners.oncologia.domain.estudio;

import java.util.List;

public interface LineaBaseRepositoryPort {

    List<LineaBaseSesion> listar(Fase fase);

    /** Reemplaza toda la linea base de la fase (una carga nueva corrige la anterior). */
    void reemplazar(Fase fase, List<LineaBaseSesion> sesiones, Long usuarioId);
}

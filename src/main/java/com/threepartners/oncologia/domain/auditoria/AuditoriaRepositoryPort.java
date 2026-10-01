package com.threepartners.oncologia.domain.auditoria;

import com.threepartners.oncologia.domain.shared.CriterioPaginacion;
import com.threepartners.oncologia.domain.shared.Pagina;

import java.time.Instant;

public interface AuditoriaRepositoryPort {

    void registrar(AuditoriaAccion accion);

    Pagina<AuditoriaAccion> buscar(Long usuarioId, String entidadAfectada, Instant desde, Instant hasta, CriterioPaginacion criterio);
}

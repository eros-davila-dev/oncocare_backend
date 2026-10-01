package com.threepartners.oncologia.application.auditoria;

import com.threepartners.oncologia.domain.auditoria.AuditoriaAccion;
import com.threepartners.oncologia.domain.auditoria.AuditoriaRepositoryPort;
import com.threepartners.oncologia.domain.shared.CriterioPaginacion;
import com.threepartners.oncologia.domain.shared.Pagina;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ConsultarAuditoriaUseCase {

    private final AuditoriaRepositoryPort auditoriaRepositoryPort;

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public Pagina<AuditoriaAccion> ejecutar(Long usuarioId, String entidadAfectada, Instant desde, Instant hasta, CriterioPaginacion criterio) {
        return auditoriaRepositoryPort.buscar(usuarioId, entidadAfectada, desde, hasta, criterio);
    }
}

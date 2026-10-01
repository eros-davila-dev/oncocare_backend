package com.threepartners.oncologia.infrastructure.in.rest.dto.auth;

import com.threepartners.oncologia.domain.usuario.Especialidad;
import com.threepartners.oncologia.domain.usuario.Rol;

public record UsuarioResumenDto(Long id, String nombres, String email, Rol rol, Especialidad especialidad) {
}

package com.threepartners.oncologia.domain.usuario;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {

    private Long id;
    private String nombres;
    private String email;
    private String passwordHash;
    private Rol rol;
    private Especialidad especialidad;
    private boolean activo;
    private Instant fechaCreacion;
    private EstadoCuenta estadoCuenta;
    private int intentosFallidos;
    private Instant bloqueadoHasta;

    public boolean tieneRol(Rol otro) {
        return this.rol == otro;
    }

    public boolean estaBloqueada() {
        return bloqueadoHasta != null && bloqueadoHasta.isAfter(Instant.now());
    }
}

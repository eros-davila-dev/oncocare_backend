package com.threepartners.oncologia.infrastructure.in.rest.dto.usuario;

import com.threepartners.oncologia.domain.usuario.Especialidad;
import com.threepartners.oncologia.domain.usuario.Rol;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CrearUsuarioRequestDto(
        @NotBlank(message = "Los nombres son obligatorios")
        String nombres,

        @NotBlank(message = "El correo electronico es obligatorio")
        @Email(message = "El correo electronico no tiene un formato valido")
        String email,

        @NotBlank(message = "La contrasena es obligatoria")
        @Size(min = 8, message = "La contrasena debe tener al menos 8 caracteres")
        String password,

        @NotNull(message = "El rol es obligatorio")
        Rol rol,

        Especialidad especialidad
) {
}

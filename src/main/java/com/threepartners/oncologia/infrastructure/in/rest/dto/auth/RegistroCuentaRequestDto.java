package com.threepartners.oncologia.infrastructure.in.rest.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegistroCuentaRequestDto(

        @NotBlank(message = "Los nombres son obligatorios")
        @Size(max = 150, message = "Los nombres no pueden superar los 150 caracteres")
        String nombres,

        @NotBlank(message = "El correo electronico es obligatorio")
        @Email(message = "El correo electronico no tiene un formato valido")
        String email,

        @NotBlank(message = "La contrasena es obligatoria")
        @Size(min = 8, max = 100, message = "La contrasena debe tener entre 8 y 100 caracteres")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "La contrasena debe incluir al menos una letra y un numero")
        String password
) {
}

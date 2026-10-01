package com.threepartners.oncologia.infrastructure.in.rest.dto.paciente;

import com.threepartners.oncologia.domain.paciente.ConvenioSeguro;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Las validaciones de formato (RF-15) se aplican aqui con Bean Validation; la
 * unicidad del documento de identidad es una regla de negocio y se valida en
 * RegistrarPacienteUseCase / ActualizarPacienteUseCase, no aqui.
 */
public record PacienteRequestDto(

        @NotBlank(message = "Los nombres son obligatorios")
        @Size(max = 100, message = "Los nombres no pueden superar los 100 caracteres")
        String nombres,

        @NotBlank(message = "Los apellidos son obligatorios")
        @Size(max = 100, message = "Los apellidos no pueden superar los 100 caracteres")
        String apellidos,

        @NotBlank(message = "El documento de identidad es obligatorio")
        @Pattern(regexp = "^[0-9]{8,12}$", message = "El documento de identidad debe tener entre 8 y 12 digitos")
        String documentoIdentidad,

        @NotNull(message = "La fecha de nacimiento es obligatoria")
        @Past(message = "La fecha de nacimiento debe ser anterior a hoy")
        LocalDate fechaNacimiento,

        @Pattern(regexp = "^[0-9]{7,15}$", message = "El telefono debe contener entre 7 y 15 digitos")
        String telefono,

        @Email(message = "El correo electronico no tiene un formato valido")
        String email,

        @Size(max = 200, message = "La direccion no puede superar los 200 caracteres")
        String direccion,

        @Size(max = 100, message = "El tipo de cancer no puede superar los 100 caracteres")
        String tipoCancer,

        @Size(max = 60, message = "El estadio clinico no puede superar los 60 caracteres")
        String estadioClinico,

        LocalDate fechaDiagnostico,

        Long medicoTratanteId,

        @NotNull(message = "El convenio o seguro es obligatorio")
        ConvenioSeguro convenioSeguro,

        @NotBlank(message = "El nombre del contacto de emergencia es obligatorio")
        String contactoEmergenciaNombre,

        @NotBlank(message = "El telefono del contacto de emergencia es obligatorio")
        @Pattern(regexp = "^[0-9]{7,15}$", message = "El telefono del contacto de emergencia debe contener entre 7 y 15 digitos")
        String contactoEmergenciaTelefono,

        @PositiveOrZero(message = "El tiempo de registro no puede ser negativo")
        Integer tiempoRegistroSegundos
) {
}

package com.threepartners.oncologia.infrastructure.in.rest.dto.paciente;

import com.threepartners.oncologia.domain.paciente.ConvenioSeguro;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
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

        // Obligatorio salvo en el portal, donde se toma el de la cuenta (ver Paciente.validarDatosDeContacto).
        @Email(message = "El correo electronico no tiene un formato valido")
        @Size(max = 150, message = "El correo electronico no puede superar los 150 caracteres")
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

        @NotBlank(message = "El correo del referido es obligatorio")
        @Email(message = "El correo del referido no tiene un formato valido")
        @Size(max = 150, message = "El correo del referido no puede superar los 150 caracteres")
        String contactoEmergenciaEmail,

        /** El paciente autoriza enviar a su referido una copia de los recordatorios de cita. */
        boolean contactoRecibeRecordatorios,

        /**
         * Sesion de medicion abierta con POST /mediciones/registro al mostrar
         * el formulario (indicador TPR). El tiempo lo mide el servidor; el
         * cliente solo informa a que sesion corresponde este guardado.
         */
        Long medicionId
) {
}

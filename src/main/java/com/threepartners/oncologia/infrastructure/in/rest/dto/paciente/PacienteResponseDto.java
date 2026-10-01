package com.threepartners.oncologia.infrastructure.in.rest.dto.paciente;

import com.threepartners.oncologia.domain.paciente.ConvenioSeguro;

import java.time.Instant;
import java.time.LocalDate;

public record PacienteResponseDto(
        Long id,
        String nombres,
        String apellidos,
        String documentoIdentidad,
        LocalDate fechaNacimiento,
        int edad,
        String telefono,
        String email,
        String direccion,
        String tipoCancer,
        String estadioClinico,
        LocalDate fechaDiagnostico,
        Long medicoTratanteId,
        ConvenioSeguro convenioSeguro,
        String contactoEmergenciaNombre,
        String contactoEmergenciaTelefono,
        boolean activo,
        Instant fechaRegistro
) {
}

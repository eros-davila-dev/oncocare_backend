package com.threepartners.oncologia.infrastructure.in.rest.dto.paciente;

import com.threepartners.oncologia.domain.paciente.ConvenioSeguro;
import com.threepartners.oncologia.domain.paciente.EstadoTratamientoPaciente;
import com.threepartners.oncologia.domain.usuario.Especialidad;

import java.time.LocalDate;

public record PacienteResumenResponseDto(
        Long id,
        String nombres,
        String apellidos,
        String documentoIdentidad,
        int edad,
        String tipoCancer,
        ConvenioSeguro convenioSeguro,
        boolean activo,
        EstadoTratamientoPaciente estadoTratamiento,
        String medicoTratanteNombre,
        Especialidad medicoTratanteEspecialidad,
        LocalDate ultimaCita,
        LocalDate proximaCita,
        boolean tieneTelegram,
        boolean referidoTieneTelegram
) {
}

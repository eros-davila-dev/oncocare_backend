package com.threepartners.oncologia.infrastructure.in.rest.dto.cita;

public record CitaAgendaResponseDto(
        CitaResponseDto cita,
        String pacienteNombre,
        String pacienteDocumento,
        String pacienteTelefono,
        String medicoNombre,
        boolean pacienteConTelegram
) {
}

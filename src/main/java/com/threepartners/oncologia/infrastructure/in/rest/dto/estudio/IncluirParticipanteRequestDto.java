package com.threepartners.oncologia.infrastructure.in.rest.dto.estudio;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record IncluirParticipanteRequestDto(

        @NotNull(message = "El paciente es obligatorio")
        Long pacienteId,

        @NotNull(message = "La fecha del consentimiento informado es obligatoria")
        @PastOrPresent(message = "La fecha del consentimiento no puede ser futura")
        LocalDate fechaConsentimiento,

        @Size(max = 500, message = "La observacion no puede superar 500 caracteres")
        String observacion
) {
}

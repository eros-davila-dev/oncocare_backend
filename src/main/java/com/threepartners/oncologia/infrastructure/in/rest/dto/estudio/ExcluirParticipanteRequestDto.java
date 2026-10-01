package com.threepartners.oncologia.infrastructure.in.rest.dto.estudio;

import com.threepartners.oncologia.domain.estudio.MotivoExclusion;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ExcluirParticipanteRequestDto(

        @NotNull(message = "El motivo de exclusion es obligatorio")
        MotivoExclusion motivo,

        @Size(max = 500, message = "La observacion no puede superar 500 caracteres")
        String observacion
) {
}

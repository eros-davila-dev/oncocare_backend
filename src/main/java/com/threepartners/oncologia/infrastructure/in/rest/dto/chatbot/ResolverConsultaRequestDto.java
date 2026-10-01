package com.threepartners.oncologia.infrastructure.in.rest.dto.chatbot;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ResolverConsultaRequestDto(

        @NotNull(message = "Indique si la consulta quedo resuelta")
        Boolean resuelta,

        @Size(max = 1000, message = "La nota no puede superar 1000 caracteres")
        String nota
) {
}

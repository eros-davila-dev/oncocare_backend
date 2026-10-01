package com.threepartners.oncologia.infrastructure.in.rest.dto.chatbot;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PreguntaFrecuenteDto(

        Long id,

        @NotBlank(message = "La pregunta es obligatoria")
        @Size(max = 200, message = "La pregunta no puede superar 200 caracteres")
        String pregunta,

        @NotBlank(message = "La respuesta es obligatoria")
        @Size(max = 2000, message = "La respuesta no puede superar 2000 caracteres")
        String respuesta,

        @Size(max = 40, message = "La categoria no puede superar 40 caracteres")
        String categoria,

        int orden,

        boolean activa
) {
}

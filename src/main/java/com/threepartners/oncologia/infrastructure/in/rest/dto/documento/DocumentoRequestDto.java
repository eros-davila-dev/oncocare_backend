package com.threepartners.oncologia.infrastructure.in.rest.dto.documento;

import com.threepartners.oncologia.domain.documento.TipoDocumento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DocumentoRequestDto(
        @NotNull(message = "El paciente es obligatorio")
        Long pacienteId,

        @NotNull(message = "El tipo de documento es obligatorio")
        TipoDocumento tipoDocumento,

        @NotBlank(message = "La url del archivo es obligatoria")
        String urlArchivo
) {
}

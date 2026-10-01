package com.threepartners.oncologia.infrastructure.in.rest.dto.documento;

import com.threepartners.oncologia.domain.documento.TipoDocumento;

import java.time.Instant;

public record DocumentoResponseDto(Long id, Long pacienteId, TipoDocumento tipoDocumento, String urlArchivo, Instant fechaCarga) {
}

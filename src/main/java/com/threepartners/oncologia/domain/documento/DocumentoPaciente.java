package com.threepartners.oncologia.domain.documento;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentoPaciente {

    private Long id;
    private Long pacienteId;
    private TipoDocumento tipoDocumento;
    private String urlArchivo;
    private Instant fechaCarga;
}

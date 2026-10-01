package com.threepartners.oncologia.infrastructure.in.rest.dto.estudio;

import com.threepartners.oncologia.domain.estudio.MotivoExclusion;
import com.threepartners.oncologia.domain.estudio.ParticipanteEstudio;
import com.threepartners.oncologia.domain.estudio.ParticipanteResumen;

import java.time.Instant;
import java.time.LocalDate;

public record ParticipanteResponseDto(
        Long id,
        Long pacienteId,
        String codigo,
        String nombreCompleto,
        String documentoIdentidad,
        LocalDate fechaConsentimiento,
        boolean incluido,
        MotivoExclusion motivoExclusion,
        String observacion,
        Instant fechaInclusion,
        Instant fechaExclusion
) {

    public static ParticipanteResponseDto de(ParticipanteResumen r) {
        ParticipanteEstudio p = r.participante();
        return new ParticipanteResponseDto(p.getId(), p.getPacienteId(), p.getCodigo(), r.nombreCompleto(),
                r.documentoIdentidad(), p.getFechaConsentimiento(), p.isIncluido(), p.getMotivoExclusion(),
                p.getObservacion(), p.getFechaInclusion(), p.getFechaExclusion());
    }

    public static ParticipanteResponseDto de(ParticipanteEstudio p) {
        return new ParticipanteResponseDto(p.getId(), p.getPacienteId(), p.getCodigo(), null, null,
                p.getFechaConsentimiento(), p.isIncluido(), p.getMotivoExclusion(), p.getObservacion(),
                p.getFechaInclusion(), p.getFechaExclusion());
    }
}

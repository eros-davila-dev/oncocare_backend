package com.threepartners.oncologia.infrastructure.in.rest.dto.estudio;

import com.threepartners.oncologia.domain.estudio.CanalMedicion;
import com.threepartners.oncologia.domain.estudio.EstadoMedicion;
import com.threepartners.oncologia.domain.estudio.MedicionRegistro;
import com.threepartners.oncologia.domain.estudio.TipoMedicion;

import java.time.Instant;

public record MedicionRegistroResponseDto(
        Long id,
        TipoMedicion tipo,
        CanalMedicion canal,
        EstadoMedicion estado,
        boolean sospechosa,
        Long usuarioId,
        Long pacienteId,
        Long entidadId,
        Instant inicio,
        Instant fin,
        Long duracionSegundos,
        String observacion
) {

    public static MedicionRegistroResponseDto de(MedicionRegistro m) {
        return new MedicionRegistroResponseDto(m.getId(), m.getTipo(), m.getCanal(), m.getEstado(), m.isSospechosa(),
                m.getUsuarioId(), m.getPacienteId(), m.getEntidadId(), m.getInicio(), m.getFin(),
                m.duracion() != null ? m.duracion().toSeconds() : null, m.getObservacion());
    }
}

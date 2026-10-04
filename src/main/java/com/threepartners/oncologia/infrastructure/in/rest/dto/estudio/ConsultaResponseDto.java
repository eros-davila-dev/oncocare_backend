package com.threepartners.oncologia.infrastructure.in.rest.dto.estudio;

import com.threepartners.oncologia.domain.estudio.CanalConsulta;
import com.threepartners.oncologia.domain.estudio.Consulta;
import com.threepartners.oncologia.domain.estudio.ResultadoConsulta;

import java.time.Instant;

public record ConsultaResponseDto(
        Long id,
        CanalConsulta canal,
        Long pacienteId,
        String intencion,
        String resumen,
        ResultadoConsulta resultado,
        Instant abiertaEn,
        Instant cerradaEn,
        Integer tiempoPrimeraRespuestaMs,
        Integer valoracion,
        Long resueltaPorUsuarioId,
        boolean capturaManual,
        String observacion,
        String pacienteNombre,
        String pacienteTelefono
) {

    public static ConsultaResponseDto de(Consulta c) {
        return new ConsultaResponseDto(c.getId(), c.getCanal(), c.getPacienteId(), c.getIntencion(), c.getResumen(),
                c.getResultado(), c.getAbiertaEn(), c.getCerradaEn(), c.getTiempoPrimeraRespuestaMs(),
                c.getValoracion(), c.getResueltaPorUsuarioId(), c.getCapturadoPor() != null, c.getObservacion(), null, null);
    }

    /** Para la bandeja del personal: con el nombre y telefono de quien consulto. */
    public static ConsultaResponseDto deBandeja(Consulta c, String pacienteNombre, String pacienteTelefono) {
        return new ConsultaResponseDto(c.getId(), c.getCanal(), c.getPacienteId(), c.getIntencion(), c.getResumen(),
                c.getResultado(), c.getAbiertaEn(), c.getCerradaEn(), c.getTiempoPrimeraRespuestaMs(),
                c.getValoracion(), c.getResueltaPorUsuarioId(), c.getCapturadoPor() != null, c.getObservacion(),
                pacienteNombre, pacienteTelefono);
    }
}

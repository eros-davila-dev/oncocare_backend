package com.threepartners.oncologia.infrastructure.in.rest.dto.estudio;

import com.threepartners.oncologia.domain.estudio.CanalConsulta;
import com.threepartners.oncologia.domain.estudio.CategoriaConsulta;
import com.threepartners.oncologia.domain.estudio.Consulta;
import com.threepartners.oncologia.domain.estudio.ResultadoConsulta;

import java.time.Instant;

/**
 * Una consulta del historial ("Mis consultas" y ficha del paciente). Sin la
 * nota interna del personal: la ve el paciente.
 */
public record HistorialConsultaDto(
        Long id,
        Instant abiertaEn,
        Instant cerradaEn,
        CanalConsulta canal,
        CategoriaConsulta categoria,
        ResultadoConsulta resultado,
        boolean derivada,
        String resumen,
        int turnos
) {

    public static HistorialConsultaDto de(Consulta c) {
        return new HistorialConsultaDto(c.getId(), c.getAbiertaEn(), c.getCerradaEn(), c.getCanal(), c.getCategoria(),
                c.getResultado(), c.isDerivada(), c.getResumen(), c.getTurnos());
    }
}

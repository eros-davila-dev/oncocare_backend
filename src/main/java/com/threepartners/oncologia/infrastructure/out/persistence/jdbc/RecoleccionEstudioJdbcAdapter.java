package com.threepartners.oncologia.infrastructure.out.persistence.jdbc;

import com.threepartners.oncologia.domain.estudio.CanalConsulta;
import com.threepartners.oncologia.domain.estudio.CanalMedicion;
import com.threepartners.oncologia.domain.estudio.CategoriaConsulta;
import com.threepartners.oncologia.domain.estudio.PeriodoMedicion;
import com.threepartners.oncologia.domain.estudio.RecoleccionEstudioRepositoryPort;
import com.threepartners.oncologia.domain.estudio.RecoleccionSesiones;
import com.threepartners.oncologia.domain.estudio.ResultadoConsulta;
import com.threepartners.oncologia.domain.shared.ZonaHoraria;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Filas de las tres fichas del Anexo 2 para la recoleccion por sesion. A
 * diferencia de IndicadoresEstudioJdbcAdapter (muestra con consentimiento,
 * diseno pareado), aqui entran todos los pacientes: en la tesis v8 la
 * unidad de analisis son los procesos de la fundacion, no un grupo fijo.
 *
 * El paciente se identifica solo con un codigo anonimo (PAC-0001, a partir
 * de su id), nunca con nombre ni documento (Ley 29733).
 */
@Component
@RequiredArgsConstructor
public class RecoleccionEstudioJdbcAdapter implements RecoleccionEstudioRepositoryPort {

    private static final String CODIGO = "CASE WHEN %s.paciente_id IS NULL THEN NULL ELSE 'PAC-' || LPAD(%s.paciente_id::text, 4, '0') END";

    // TPR: alta de paciente por el personal (INTRANET) o capturada en la ficha
    // del sistema (MANUAL). No entra el autorregistro del portal.
    private static final String TIEMPOS = """
            SELECT %s AS codigo, m.inicio, m.fin, m.duracion_segundos, m.canal, m.sospechosa
            FROM medicion_registro m
            WHERE m.estado = 'COMPLETADA'
              AND m.tipo = 'REGISTRO_PACIENTE'
              AND m.canal IN ('INTRANET', 'MANUAL')
              AND m.inicio >= :desde AND m.inicio < :hasta
            ORDER BY m.inicio, m.id
            """.formatted(CODIGO.formatted("m", "m"));

    // Todas las citas del periodo por su fecha (final, si se reprogramo); la
    // elegibilidad la decide RecoleccionSesiones.
    private static final String CITAS = """
            SELECT %s AS codigo, c.fecha, c.hora, c.estado, c.veces_reprogramada,
                   EXISTS (SELECT 1 FROM recordatorio r WHERE r.cita_id = c.id AND r.estado = 'ENVIADO') AS recordatorio
            FROM cita c
            WHERE c.fecha BETWEEN :fechaDesde AND :fechaHasta
            ORDER BY c.fecha, c.hora, c.id
            """.formatted(CODIGO.formatted("c", "c"));

    // Las anuladas (consulta duplicada o invalida) se excluyen, como en la tesis.
    private static final String CONSULTAS = """
            SELECT %s AS codigo, k.abierta_en, k.canal, k.categoria, k.resultado, k.derivada, k.reabierta,
                   k.tiempo_primera_respuesta_ms
            FROM consulta k
            WHERE k.abierta_en >= :desde AND k.abierta_en < :hasta
              AND (k.resultado IS NULL OR k.resultado <> 'ANULADA')
            ORDER BY k.abierta_en, k.id
            """.formatted(CODIGO.formatted("k", "k"));

    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public RecoleccionSesiones.Detalle filas(PeriodoMedicion periodo) {
        var params = new MapSqlParameterSource()
                .addValue("desde", OffsetDateTime.ofInstant(periodo.inicio(), ZoneOffset.UTC))
                .addValue("hasta", OffsetDateTime.ofInstant(periodo.finExclusivo(), ZoneOffset.UTC))
                .addValue("fechaDesde", periodo.desde())
                .addValue("fechaHasta", periodo.hasta());

        var tiempos = jdbc.query(TIEMPOS, params, (rs, i) -> {
            ZonedDateTime inicio = enLima(rs.getTimestamp("inicio"));
            return new RecoleccionSesiones.FilaTiempo(rs.getString("codigo"), inicio.toLocalDate(),
                    inicio.toLocalTime(), enLima(rs.getTimestamp("fin")).toLocalTime(),
                    rs.getLong("duracion_segundos") / 60.0, CanalMedicion.valueOf(rs.getString("canal")),
                    rs.getBoolean("sospechosa"));
        });
        var citas = jdbc.query(CITAS, params, (rs, i) -> new RecoleccionSesiones.FilaCita(
                rs.getString("codigo"), rs.getObject("fecha", LocalDate.class), rs.getObject("hora", LocalTime.class),
                rs.getString("estado"), rs.getBoolean("recordatorio"), rs.getInt("veces_reprogramada")));
        var consultas = jdbc.query(CONSULTAS, params, (rs, i) -> {
            ZonedDateTime abierta = enLima(rs.getTimestamp("abierta_en"));
            String resultado = rs.getString("resultado");
            int ms = rs.getInt("tiempo_primera_respuesta_ms");
            Double minutos = rs.wasNull() ? null : ms / 60_000.0;
            return new RecoleccionSesiones.FilaConsulta(rs.getString("codigo"), abierta.toLocalDate(),
                    abierta.toLocalTime(), CategoriaConsulta.desde(rs.getString("categoria")),
                    CanalConsulta.valueOf(rs.getString("canal")),
                    resultado != null ? ResultadoConsulta.valueOf(resultado) : null,
                    rs.getBoolean("derivada"), rs.getBoolean("reabierta"), minutos);
        });
        return new RecoleccionSesiones.Detalle(tiempos, citas, consultas);
    }

    private static ZonedDateTime enLima(Timestamp instante) {
        return instante.toInstant().atZone(ZonaHoraria.LIMA).truncatedTo(ChronoUnit.SECONDS);
    }
}

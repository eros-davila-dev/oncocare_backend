package com.threepartners.oncologia.infrastructure.out.persistence.jdbc;

import com.threepartners.oncologia.domain.dashboard.ActividadPeriodo;
import com.threepartners.oncologia.domain.dashboard.ActividadRepositoryPort;
import com.threepartners.oncologia.domain.estudio.CanalConsulta;
import com.threepartners.oncologia.domain.estudio.CanalMedicion;
import com.threepartners.oncologia.domain.estudio.CategoriaConsulta;
import com.threepartners.oncologia.domain.estudio.PeriodoMedicion;
import com.threepartners.oncologia.domain.estudio.ResultadoConsulta;
import com.threepartners.oncologia.domain.shared.ZonaHoraria;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Actividad del periodo para el panel del personal. Mismos criterios que
 * RecoleccionEstudioJdbcAdapter (que registros cuentan para el TPR, que
 * consultas se excluyen), pero con el nombre del paciente en vez del codigo
 * anonimo: lo ve el personal, no se exporta para la investigacion.
 */
@Component
@RequiredArgsConstructor
public class ActividadJdbcAdapter implements ActividadRepositoryPort {

    private static final String NOMBRE = "CASE WHEN p.id IS NULL THEN NULL ELSE p.nombres || ' ' || p.apellidos END";

    private static final String REGISTROS = """
            SELECT %s AS paciente, u.nombres AS registrado_por, m.inicio, m.duracion_segundos, m.canal, m.sospechosa
            FROM medicion_registro m
            LEFT JOIN paciente p ON p.id = m.paciente_id
            LEFT JOIN usuario u ON u.id = m.usuario_id
            WHERE m.estado = 'COMPLETADA'
              AND m.tipo = 'REGISTRO_PACIENTE'
              AND m.canal IN ('INTRANET', 'MANUAL')
              AND m.inicio >= :desde AND m.inicio < :hasta
            ORDER BY m.inicio, m.id
            """.formatted(NOMBRE);

    private static final String CITAS = """
            SELECT %s AS paciente, u.nombres AS medico, c.fecha, c.hora, c.estado, c.veces_reprogramada,
                   EXISTS (SELECT 1 FROM recordatorio r WHERE r.cita_id = c.id AND r.estado = 'ENVIADO') AS recordatorio
            FROM cita c
            LEFT JOIN paciente p ON p.id = c.paciente_id
            LEFT JOIN usuario u ON u.id = c.medico_id
            WHERE c.fecha BETWEEN :fechaDesde AND :fechaHasta
            ORDER BY c.fecha, c.hora, c.id
            """.formatted(NOMBRE);

    // Recordatorios de las citas del periodo (por la fecha de la cita).
    private static final String RECORDATORIOS = """
            SELECT %s AS paciente, c.fecha AS fecha_cita, r.tipo, r.canal, r.estado, r.programado_para,
                   r.enviado_en, r.respuesta
            FROM recordatorio r
            JOIN cita c ON c.id = r.cita_id
            LEFT JOIN paciente p ON p.id = c.paciente_id
            WHERE c.fecha BETWEEN :fechaDesde AND :fechaHasta
              AND r.estado <> 'CANCELADO'
            ORDER BY r.programado_para, r.id
            """.formatted(NOMBRE);

    private static final String CONSULTAS = """
            SELECT %s AS paciente, k.abierta_en, k.canal, k.categoria, k.resultado, k.derivada, k.reabierta,
                   k.tiempo_primera_respuesta_ms
            FROM consulta k
            LEFT JOIN paciente p ON p.id = k.paciente_id
            WHERE k.abierta_en >= :desde AND k.abierta_en < :hasta
              AND (k.resultado IS NULL OR k.resultado <> 'ANULADA')
            ORDER BY k.abierta_en, k.id
            """.formatted(NOMBRE);

    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public ActividadPeriodo.Filas filas(PeriodoMedicion periodo) {
        var params = new MapSqlParameterSource()
                .addValue("desde", OffsetDateTime.ofInstant(periodo.inicio(), ZoneOffset.UTC))
                .addValue("hasta", OffsetDateTime.ofInstant(periodo.finExclusivo(), ZoneOffset.UTC))
                .addValue("fechaDesde", periodo.desde())
                .addValue("fechaHasta", periodo.hasta());

        var registros = jdbc.query(REGISTROS, params, (rs, i) -> {
            ZonedDateTime inicio = enLima(rs.getTimestamp("inicio"));
            return new ActividadPeriodo.Registro(rs.getString("paciente"), rs.getString("registrado_por"),
                    inicio.toLocalDate(), inicio.toLocalTime(), rs.getLong("duracion_segundos") / 60.0,
                    CanalMedicion.valueOf(rs.getString("canal")), rs.getBoolean("sospechosa"));
        });
        var citas = jdbc.query(CITAS, params, (rs, i) -> new ActividadPeriodo.Cita(
                rs.getString("paciente"), rs.getString("medico"), rs.getObject("fecha", LocalDate.class),
                rs.getObject("hora", LocalTime.class), rs.getString("estado"), rs.getBoolean("recordatorio"),
                rs.getInt("veces_reprogramada")));
        var recordatorios = jdbc.query(RECORDATORIOS, params, (rs, i) -> new ActividadPeriodo.Recordatorio(
                rs.getString("paciente"), rs.getObject("fecha_cita", LocalDate.class), rs.getString("tipo"),
                rs.getString("canal"), rs.getString("estado"), local(rs.getTimestamp("programado_para")),
                local(rs.getTimestamp("enviado_en")), rs.getString("respuesta")));
        var consultas = jdbc.query(CONSULTAS, params, (rs, i) -> {
            ZonedDateTime abierta = enLima(rs.getTimestamp("abierta_en"));
            String resultado = rs.getString("resultado");
            int ms = rs.getInt("tiempo_primera_respuesta_ms");
            Double minutos = rs.wasNull() ? null : ms / 60_000.0;
            return new ActividadPeriodo.Consulta(rs.getString("paciente"), abierta.toLocalDate(),
                    abierta.toLocalTime(), CategoriaConsulta.desde(rs.getString("categoria")),
                    CanalConsulta.valueOf(rs.getString("canal")),
                    resultado != null ? ResultadoConsulta.valueOf(resultado) : null,
                    rs.getBoolean("derivada"), rs.getBoolean("reabierta"), minutos);
        });
        return new ActividadPeriodo.Filas(registros, citas, recordatorios, consultas);
    }

    private static ZonedDateTime enLima(Timestamp instante) {
        return instante.toInstant().atZone(ZonaHoraria.LIMA).truncatedTo(ChronoUnit.SECONDS);
    }

    private static LocalDateTime local(Timestamp instante) {
        return instante == null ? null : enLima(instante).toLocalDateTime();
    }
}

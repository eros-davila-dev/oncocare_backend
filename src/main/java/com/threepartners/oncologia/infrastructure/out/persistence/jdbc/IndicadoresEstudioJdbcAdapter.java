package com.threepartners.oncologia.infrastructure.out.persistence.jdbc;

import com.threepartners.oncologia.domain.estudio.AlcanceIndicador;
import com.threepartners.oncologia.domain.estudio.CanalConsulta;
import com.threepartners.oncologia.domain.estudio.CanalMedicion;
import com.threepartners.oncologia.domain.estudio.ConteosIndicadores;
import com.threepartners.oncologia.domain.estudio.FiltroIndicadores;
import com.threepartners.oncologia.domain.estudio.IndicadoresEstudioRepositoryPort;
import com.threepartners.oncologia.domain.estudio.RegistrosEstudioRepositoryPort;
import com.threepartners.oncologia.domain.estudio.RegistrosFichas;
import com.threepartners.oncologia.domain.estudio.ResultadoConsulta;
import com.threepartners.oncologia.domain.estudio.TipoMedicion;
import com.threepartners.oncologia.domain.shared.ZonaHoraria;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Conteos de los indicadores de la tesis con SQL agregado. Las definiciones
 * operativas (que cuenta y que no) estan aqui, en un solo lugar, y estan
 * cubiertas por IndicadoresEstudioJdbcAdapterIntegracionTest con un escenario conocido:
 *
 * - TPR: mediciones COMPLETADA del tipo y canales pedidos, por fecha de inicio.
 *        ABANDONADA y ANULADA no cuentan.
 * - TNS: citas con desenlace (ATENDIDA / NO_ASISTIO) por FECHA DE LA CITA.
 *        Canceladas, programadas o confirmadas no entran al denominador.
 * - NCA: consultas con resultado final (RESUELTA_BOT, RESUELTA_PERSONAL,
 *        NO_RESUELTA) por fecha de apertura. Abiertas, ESCALADA y ANULADA no cuentan.
 *
 * Tambien entrega esas mismas filas una por una para exportar las fichas
 * (sin datos personales: solo el codigo de participante).
 */
@Component
@RequiredArgsConstructor
public class IndicadoresEstudioJdbcAdapter implements IndicadoresEstudioRepositoryPort, RegistrosEstudioRepositoryPort {

    private static final String JOIN_MUESTRA =
            " JOIN participante_estudio pe ON pe.paciente_id = %s.paciente_id AND pe.incluido ";

    /** Fuera de la muestra el codigo sale null (exportacion GLOBAL). */
    private static final String LEFT_JOIN_MUESTRA =
            " LEFT JOIN participante_estudio pe ON pe.paciente_id = %s.paciente_id AND pe.incluido ";

    // Que filas entran a cada indicador. Los conteos y la exportacion fila por
    // fila comparten estos fragmentos para que nunca se desalineen.
    private static final String ORIGEN_TPR = """
            FROM medicion_registro m %s
            WHERE m.estado = 'COMPLETADA'
              AND m.tipo = :tipo
              AND m.canal IN (:canales)
              AND m.inicio >= :desde AND m.inicio < :hasta
            """;

    private static final String ORIGEN_TNS = """
            FROM cita c %s
            WHERE c.fecha BETWEEN :fechaDesde AND :fechaHasta
              AND c.estado IN ('ATENDIDA', 'NO_ASISTIO')
            """;

    private static final String ORIGEN_NCA = """
            FROM consulta k %s
            WHERE k.resultado IN ('RESUELTA_BOT', 'RESUELTA_PERSONAL', 'NO_RESUELTA')
              AND k.abierta_en >= :desde AND k.abierta_en < :hasta
            """;

    private static final String SQL_TPR = """
            SELECT %s COUNT(*) AS registros, COALESCE(SUM(m.duracion_segundos), 0) AS suma_segundos
            """ + ORIGEN_TPR + "%s";

    private static final String SQL_TNS = """
            SELECT %s COUNT(*) FILTER (WHERE c.estado = 'NO_ASISTIO') AS inasistencias,
                      COUNT(*) FILTER (WHERE c.estado = 'ATENDIDA')   AS cumplidas
            """ + ORIGEN_TNS + "%s";

    private static final String SQL_NCA = """
            SELECT %s COUNT(*) FILTER (WHERE k.resultado IN ('RESUELTA_BOT', 'RESUELTA_PERSONAL')) AS resueltas,
                      COUNT(*) FILTER (WHERE k.resultado = 'RESUELTA_BOT')                       AS resueltas_bot,
                      COUNT(*)                                                                    AS cerradas
            """ + ORIGEN_NCA + "%s";

    private static final String FILAS_TPR = "SELECT pe.codigo, m.inicio, m.fin, m.duracion_segundos, m.tipo, m.canal "
            + ORIGEN_TPR + " ORDER BY m.inicio, m.id";

    private static final String FILAS_TNS = "SELECT pe.codigo, c.fecha, c.hora, c.estado, c.origen "
            + ORIGEN_TNS + " ORDER BY c.fecha, c.hora, c.id";

    private static final String FILAS_NCA = "SELECT pe.codigo, k.abierta_en, k.canal, k.resultado "
            + ORIGEN_NCA + " ORDER BY k.abierta_en, k.id";

    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public ConteosIndicadores contar(FiltroIndicadores filtro) {
        boolean soloMuestra = filtro.alcance() == AlcanceIndicador.MUESTRA;
        var params = parametros(filtro);

        long[] tpr = jdbc.queryForObject(sql(SQL_TPR, "m", soloMuestra, false), params,
                (rs, i) -> new long[]{rs.getLong("registros"), rs.getLong("suma_segundos")});
        long[] tns = jdbc.queryForObject(sql(SQL_TNS, "c", soloMuestra, false), params,
                (rs, i) -> new long[]{rs.getLong("inasistencias"), rs.getLong("cumplidas")});
        long[] nca = jdbc.queryForObject(sql(SQL_NCA, "k", soloMuestra, false), params,
                (rs, i) -> new long[]{rs.getLong("resueltas"), rs.getLong("resueltas_bot"), rs.getLong("cerradas")});

        return new ConteosIndicadores(tpr[0], tpr[1], tns[0], tns[1], nca[0], nca[1], nca[2]);
    }

    @Override
    public Map<String, ConteosIndicadores> contarPorParticipante(FiltroIndicadores filtro) {
        var params = parametros(filtro);
        Map<String, long[]> acumulado = new HashMap<>();

        jdbc.query(sql(SQL_TPR, "m", true, true), params, (ResultSet rs) -> {
            long[] fila = fila(acumulado, rs);
            fila[0] = rs.getLong("registros");
            fila[1] = rs.getLong("suma_segundos");
        });
        jdbc.query(sql(SQL_TNS, "c", true, true), params, (ResultSet rs) -> {
            long[] fila = fila(acumulado, rs);
            fila[2] = rs.getLong("inasistencias");
            fila[3] = rs.getLong("cumplidas");
        });
        jdbc.query(sql(SQL_NCA, "k", true, true), params, (ResultSet rs) -> {
            long[] fila = fila(acumulado, rs);
            fila[4] = rs.getLong("resueltas");
            fila[5] = rs.getLong("resueltas_bot");
            fila[6] = rs.getLong("cerradas");
        });

        Map<String, ConteosIndicadores> resultado = new TreeMap<>();
        acumulado.forEach((codigo, v) ->
                resultado.put(codigo, new ConteosIndicadores(v[0], v[1], v[2], v[3], v[4], v[5], v[6])));
        return resultado;
    }

    @Override
    public List<RegistrosFichas.Tiempo> tiempos(FiltroIndicadores filtro) {
        return jdbc.query(filas(FILAS_TPR, "m", filtro), parametros(filtro), (rs, i) -> {
            ZonedDateTime inicio = enLima(rs.getTimestamp("inicio"));
            return new RegistrosFichas.Tiempo(rs.getString("codigo"), inicio.toLocalDate(), inicio.toLocalTime(),
                    enLima(rs.getTimestamp("fin")).toLocalTime(), rs.getLong("duracion_segundos"),
                    TipoMedicion.valueOf(rs.getString("tipo")), CanalMedicion.valueOf(rs.getString("canal")));
        });
    }

    @Override
    public List<RegistrosFichas.Asistencia> asistencias(FiltroIndicadores filtro) {
        return jdbc.query(filas(FILAS_TNS, "c", filtro), parametros(filtro), (rs, i) ->
                new RegistrosFichas.Asistencia(rs.getString("codigo"), rs.getObject("fecha", LocalDate.class),
                        rs.getObject("hora", LocalTime.class), "ATENDIDA".equals(rs.getString("estado")),
                        rs.getString("origen")));
    }

    @Override
    public List<RegistrosFichas.ConsultaCerrada> consultas(FiltroIndicadores filtro) {
        return jdbc.query(filas(FILAS_NCA, "k", filtro), parametros(filtro), (rs, i) -> {
            ZonedDateTime abierta = enLima(rs.getTimestamp("abierta_en"));
            return new RegistrosFichas.ConsultaCerrada(rs.getString("codigo"), abierta.toLocalDate(),
                    abierta.toLocalTime(), CanalConsulta.valueOf(rs.getString("canal")),
                    ResultadoConsulta.valueOf(rs.getString("resultado")));
        });
    }

    private static String filas(String plantilla, String alias, FiltroIndicadores filtro) {
        String join = filtro.alcance() == AlcanceIndicador.MUESTRA ? JOIN_MUESTRA : LEFT_JOIN_MUESTRA;
        return plantilla.formatted(join.formatted(alias));
    }

    private static ZonedDateTime enLima(Timestamp instante) {
        return instante.toInstant().atZone(ZonaHoraria.LIMA).truncatedTo(ChronoUnit.SECONDS);
    }

    private static long[] fila(Map<String, long[]> acumulado, ResultSet rs) throws SQLException {
        return acumulado.computeIfAbsent(rs.getString("codigo"), c -> new long[7]);
    }

    private static String sql(String plantilla, String alias, boolean soloMuestra, boolean porParticipante) {
        String columnaCodigo = porParticipante ? "pe.codigo AS codigo," : "";
        String join = soloMuestra || porParticipante ? JOIN_MUESTRA.formatted(alias) : "";
        String agrupacion = porParticipante ? "GROUP BY pe.codigo" : "";
        return plantilla.formatted(columnaCodigo, join, agrupacion);
    }

    private static MapSqlParameterSource parametros(FiltroIndicadores filtro) {
        return new MapSqlParameterSource()
                .addValue("tipo", filtro.tipoRegistro().name())
                .addValue("canales", filtro.canalesRegistro().stream().map(CanalMedicion::name).toList())
                .addValue("desde", OffsetDateTime.ofInstant(filtro.periodo().inicio(), ZoneOffset.UTC))
                .addValue("hasta", OffsetDateTime.ofInstant(filtro.periodo().finExclusivo(), ZoneOffset.UTC))
                .addValue("fechaDesde", filtro.periodo().desde())
                .addValue("fechaHasta", filtro.periodo().hasta());
    }
}

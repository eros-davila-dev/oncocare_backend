package com.threepartners.oncologia.infrastructure.out.persistence.jdbc;

import com.threepartners.oncologia.domain.estudio.Fase;
import com.threepartners.oncologia.domain.estudio.LineaBaseRepositoryPort;
import com.threepartners.oncologia.domain.estudio.LineaBaseSesion;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class LineaBaseJdbcAdapter implements LineaBaseRepositoryPort {

    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public List<LineaBaseSesion> listar(Fase fase) {
        return jdbc.query("""
                SELECT fase, numero, fecha, tpr_min, ta_pct, nca_pct, fuente, cargado_en
                FROM linea_base_sesion WHERE fase = :fase ORDER BY numero
                """, new MapSqlParameterSource("fase", fase.name()), (rs, i) -> new LineaBaseSesion(
                Fase.valueOf(rs.getString("fase")), rs.getInt("numero"), rs.getObject("fecha", LocalDate.class),
                decimal(rs, "tpr_min"), decimal(rs, "ta_pct"), decimal(rs, "nca_pct"), rs.getString("fuente"),
                rs.getTimestamp("cargado_en").toInstant()));
    }

    @Override
    public void reemplazar(Fase fase, List<LineaBaseSesion> sesiones, Long usuarioId) {
        jdbc.update("DELETE FROM linea_base_sesion WHERE fase = :fase", new MapSqlParameterSource("fase", fase.name()));
        for (LineaBaseSesion s : sesiones) {
            jdbc.update("""
                    INSERT INTO linea_base_sesion (fase, numero, fecha, tpr_min, ta_pct, nca_pct, fuente, cargado_por, cargado_en)
                    VALUES (:fase, :numero, :fecha, :tpr, :ta, :nca, :fuente, :usuario, :cargadoEn)
                    """, new MapSqlParameterSource()
                    .addValue("fase", fase.name())
                    .addValue("numero", s.numero())
                    .addValue("fecha", s.fecha())
                    .addValue("tpr", s.tprMin())
                    .addValue("ta", s.taPct())
                    .addValue("nca", s.ncaPct())
                    .addValue("fuente", s.fuente())
                    .addValue("usuario", usuarioId)
                    .addValue("cargadoEn", Timestamp.from(s.cargadoEn())));
        }
    }

    private static Double decimal(ResultSet rs, String columna) throws SQLException {
        BigDecimal valor = rs.getBigDecimal(columna);
        return valor == null ? null : valor.doubleValue();
    }
}

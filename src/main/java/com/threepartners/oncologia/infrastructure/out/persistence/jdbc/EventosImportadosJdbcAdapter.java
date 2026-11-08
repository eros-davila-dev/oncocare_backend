package com.threepartners.oncologia.infrastructure.out.persistence.jdbc;

import com.threepartners.oncologia.domain.estudio.EventoImportado;
import com.threepartners.oncologia.domain.estudio.EventosImportadosRepositoryPort;
import com.threepartners.oncologia.domain.estudio.Fase;
import com.threepartners.oncologia.domain.estudio.TipoEventoImportado;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class EventosImportadosJdbcAdapter implements EventosImportadosRepositoryPort {

    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public void reemplazar(List<EventoImportado> eventos, Long usuarioId) {
        jdbc.getJdbcTemplate().update("DELETE FROM evento_importado");
        SqlParameterSource[] lote = eventos.stream().map(e -> new MapSqlParameterSource()
                .addValue("fase", e.fase().name())
                .addValue("tipo", e.tipo().name())
                .addValue("numero", e.numero())
                .addValue("fecha", e.fecha())
                .addValue("codigo", e.codigoPaciente())
                .addValue("inicio", e.horaInicio())
                .addValue("fin", e.horaFin())
                .addValue("minutos", e.minutos())
                .addValue("modalidad", e.modalidad())
                .addValue("estado", e.estado())
                .addValue("motivo", e.motivo())
                .addValue("recordatorio", e.recordatorio())
                .addValue("categoria", e.categoria())
                .addValue("canal", e.canal())
                .addValue("resuelta", e.resuelta())
                .addValue("derivada", e.derivada())
                .addValue("tiempo", e.tiempoRespuesta())
                .addValue("control", e.controlCalidad())
                .addValue("fuente", e.fuente())
                .addValue("usuario", usuarioId)).toArray(SqlParameterSource[]::new);
        jdbc.batchUpdate("""
                INSERT INTO evento_importado (fase, tipo, numero, fecha, codigo_paciente, hora_inicio, hora_fin, minutos,
                    modalidad, estado, motivo, recordatorio, categoria, canal, resuelta, derivada, tiempo_respuesta,
                    control_calidad, fuente, cargado_por)
                VALUES (:fase, :tipo, :numero, :fecha, :codigo, :inicio, :fin, :minutos, :modalidad, :estado, :motivo,
                    :recordatorio, :categoria, :canal, :resuelta, :derivada, :tiempo, :control, :fuente, :usuario)
                """, lote);
    }

    @Override
    public List<EventoImportado> listarEntre(LocalDate desde, LocalDate hasta) {
        return jdbc.query("SELECT * FROM evento_importado WHERE fecha BETWEEN :desde AND :hasta ORDER BY fecha, tipo, numero",
                new MapSqlParameterSource().addValue("desde", desde).addValue("hasta", hasta), (rs, i) -> fila(rs));
    }

    @Override
    public List<EventoImportado> listar(TipoEventoImportado tipo, Fase fase) {
        return jdbc.query("""
                SELECT * FROM evento_importado WHERE tipo = :tipo AND fase = :fase ORDER BY fecha, numero
                """, new MapSqlParameterSource().addValue("tipo", tipo.name()).addValue("fase", fase.name()), (rs, i) -> fila(rs));
    }

    private static EventoImportado fila(ResultSet rs) throws SQLException {
        return new EventoImportado(Fase.valueOf(rs.getString("fase")), TipoEventoImportado.valueOf(rs.getString("tipo")),
                        rs.getInt("numero"), rs.getObject("fecha", LocalDate.class), rs.getString("codigo_paciente"),
                        rs.getObject("hora_inicio", LocalTime.class), rs.getObject("hora_fin", LocalTime.class),
                        decimal(rs, "minutos"), rs.getString("modalidad"), rs.getString("estado"), rs.getString("motivo"),
                        booleano(rs, "recordatorio"), rs.getString("categoria"), rs.getString("canal"),
                        booleano(rs, "resuelta"), booleano(rs, "derivada"), decimal(rs, "tiempo_respuesta"),
                        rs.getString("control_calidad"), rs.getString("fuente"));
    }

    private static Double decimal(ResultSet rs, String columna) throws SQLException {
        BigDecimal valor = rs.getBigDecimal(columna);
        return valor == null ? null : valor.doubleValue();
    }

    private static Boolean booleano(ResultSet rs, String columna) throws SQLException {
        boolean valor = rs.getBoolean(columna);
        return rs.wasNull() ? null : valor;
    }
}

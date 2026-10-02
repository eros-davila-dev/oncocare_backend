package com.threepartners.oncologia.infrastructure.out.persistence.jdbc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.threepartners.oncologia.domain.notificacion.EstadoEventoSaliente;
import com.threepartners.oncologia.domain.notificacion.EventoSaliente;
import com.threepartners.oncologia.domain.notificacion.EventoSalienteRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Outbox con SQL nativo: JSONB para el payload y un reclamo atomico
 * (UPDATE ... WHERE id IN (SELECT ... FOR UPDATE SKIP LOCKED) RETURNING) que
 * permite varias instancias del backend sin enviar dos veces el mismo aviso.
 * Usa la conexion de la transaccion en curso, asi que encolar participa de
 * la transaccion JPA del caso de uso.
 */
@Component
@RequiredArgsConstructor
public class EventoSalienteJdbcAdapter implements EventoSalienteRepositoryPort {

    private static final TypeReference<Map<String, Object>> MAPA = new TypeReference<>() {
    };

    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    @Override
    public void encolar(String destino, Map<String, Object> payload, Instant ahora) {
        jdbc.update("""
                INSERT INTO evento_saliente (destino, payload, creado_en, proximo_intento_en)
                VALUES (:destino, CAST(:payload AS jsonb), :ahora, :ahora)
                """, new MapSqlParameterSource()
                .addValue("destino", destino)
                .addValue("payload", aJson(payload))
                .addValue("ahora", utc(ahora)));
    }

    @Override
    public List<EventoSaliente> reclamarPendientes(int limite, Instant ahora) {
        return jdbc.query("""
                UPDATE evento_saliente SET estado = 'EN_ENVIO', tomado_en = :ahora
                WHERE id IN (SELECT id FROM evento_saliente
                             WHERE estado = 'PENDIENTE' AND proximo_intento_en <= :ahora
                             ORDER BY id
                             LIMIT :limite
                             FOR UPDATE SKIP LOCKED)
                RETURNING id, destino, payload::text AS payload, intentos
                """, new MapSqlParameterSource().addValue("ahora", utc(ahora)).addValue("limite", limite),
                (rs, i) -> new EventoSaliente(rs.getLong("id"), rs.getString("destino"),
                        deJson(rs.getString("payload")), rs.getInt("intentos")))
                .stream()
                .sorted((a, b) -> Long.compare(a.id(), b.id()))
                .toList();
    }

    @Override
    public void marcarEnviado(long id, Instant ahora) {
        jdbc.update("""
                UPDATE evento_saliente
                SET estado = 'ENVIADO', enviado_en = :ahora, payload = '{}'::jsonb, ultimo_error = NULL
                WHERE id = :id
                """, new MapSqlParameterSource().addValue("id", id).addValue("ahora", utc(ahora)));
    }

    @Override
    public void reprogramar(long id, int intentos, Instant proximoIntento, String error) {
        jdbc.update("""
                UPDATE evento_saliente
                SET estado = 'PENDIENTE', intentos = :intentos, proximo_intento_en = :proximo,
                    tomado_en = NULL, ultimo_error = :error
                WHERE id = :id
                """, new MapSqlParameterSource().addValue("id", id).addValue("intentos", intentos)
                .addValue("proximo", utc(proximoIntento)).addValue("error", error));
    }

    @Override
    public void marcarFallido(long id, int intentos, String error) {
        jdbc.update("""
                UPDATE evento_saliente
                SET estado = 'FALLIDO', intentos = :intentos, payload = '{}'::jsonb, ultimo_error = :error
                WHERE id = :id
                """, new MapSqlParameterSource().addValue("id", id).addValue("intentos", intentos)
                .addValue("error", error));
    }

    @Override
    public int liberarColgados(Instant tomadosAntesDe) {
        return jdbc.update("""
                UPDATE evento_saliente SET estado = 'PENDIENTE', tomado_en = NULL
                WHERE estado = 'EN_ENVIO' AND tomado_en < :limite
                """, new MapSqlParameterSource("limite", utc(tomadosAntesDe)));
    }

    @Override
    public int purgarEnviados(Instant enviadosAntesDe) {
        return jdbc.update("DELETE FROM evento_saliente WHERE estado = 'ENVIADO' AND enviado_en < :limite",
                new MapSqlParameterSource("limite", utc(enviadosAntesDe)));
    }

    @Override
    public Map<EstadoEventoSaliente, Long> contarPorEstado() {
        Map<EstadoEventoSaliente, Long> conteo = new EnumMap<>(EstadoEventoSaliente.class);
        for (EstadoEventoSaliente estado : EstadoEventoSaliente.values()) {
            conteo.put(estado, 0L);
        }
        jdbc.query("SELECT estado, COUNT(*) AS n FROM evento_saliente GROUP BY estado", rs -> {
            conteo.put(EstadoEventoSaliente.valueOf(rs.getString("estado")), rs.getLong("n"));
        });
        return conteo;
    }

    private String aJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("El aviso para n8n no se puede serializar a JSON", e);
        }
    }

    private Map<String, Object> deJson(String json) {
        try {
            return objectMapper.readValue(json, MAPA);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Payload corrupto en el outbox", e);
        }
    }

    private static OffsetDateTime utc(Instant instante) {
        return OffsetDateTime.ofInstant(instante, ZoneOffset.UTC);
    }
}

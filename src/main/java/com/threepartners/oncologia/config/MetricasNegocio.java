package com.threepartners.oncologia.config;

import com.threepartners.oncologia.domain.notificacion.EstadoEventoSaliente;
import com.threepartners.oncologia.domain.notificacion.EventoSalienteRepositoryPort;
import com.threepartners.oncologia.domain.shared.ZonaHoraria;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;

/**
 * Metricas de negocio para Prometheus: la calidad del dato que alimenta los
 * indicadores de la tesis (lo que se vigila cada semana en el postest) y la
 * salud de la mensajeria. Solo conteos: ninguna etiqueta lleva datos de
 * pacientes. Se calculan al momento de cada lectura con consultas sobre
 * indices parciales.
 */
@Component
@RequiredArgsConstructor
public class MetricasNegocio implements MeterBinder {

    private final JdbcTemplate jdbc;
    private final EventoSalienteRepositoryPort eventoSalienteRepositoryPort;
    private final Clock clock;

    @Override
    public void bindTo(@NonNull MeterRegistry registry) {
        for (EstadoEventoSaliente estado : EstadoEventoSaliente.values()) {
            Gauge.builder("oncologia.outbox.eventos", () -> eventoSalienteRepositoryPort.contarPorEstado().get(estado))
                    .description("Avisos a n8n en el outbox por estado (FALLIDO > 0 requiere revisar n8n)")
                    .tag("estado", estado.name())
                    .register(registry);
        }
        Gauge.builder("oncologia.consultas.escaladas.pendientes",
                        () -> contar("SELECT COUNT(*) FROM consulta WHERE resultado = 'ESCALADA'"))
                .description("Consultas derivadas al personal que esperan respuesta (NCA)")
                .register(registry);
        Gauge.builder("oncologia.citas.sin.desenlace",
                        () -> contar("SELECT COUNT(*) FROM cita WHERE estado IN ('PROGRAMADA', 'CONFIRMADA') AND fecha < ?",
                                LocalDate.now(clock.withZone(ZonaHoraria.LIMA))))
                .description("Citas pasadas sin marcar atendida o no asistio (sesgan la tasa de ausentismo)")
                .register(registry);
        Gauge.builder("oncologia.mediciones.sospechosas",
                        () -> contar("SELECT COUNT(*) FROM medicion_registro WHERE sospechosa AND estado = 'COMPLETADA'"))
                .description("Registros con duracion fuera de rango que entran al TPR")
                .register(registry);
        Gauge.builder("oncologia.recordatorios.fallidos",
                        () -> contar("SELECT COUNT(*) FROM recordatorio WHERE estado = 'FALLIDO'"))
                .description("Recordatorios de Telegram que no se pudieron enviar (afecta la tasa de ausentismo)")
                .register(registry);
    }

    private double contar(String sql, Object... parametros) {
        Long n = jdbc.queryForObject(sql, Long.class, parametros);
        return n != null ? n : 0;
    }
}

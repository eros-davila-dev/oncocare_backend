package com.threepartners.oncologia.application.estudio;

import com.threepartners.oncologia.domain.auditoria.event.OperacionAuditadaEvent;
import com.threepartners.oncologia.domain.estudio.EventoImportado;
import com.threepartners.oncologia.domain.estudio.EventosImportadosRepositoryPort;
import com.threepartners.oncologia.domain.estudio.Fase;
import com.threepartners.oncologia.domain.estudio.TipoEventoImportado;
import com.threepartners.oncologia.domain.shared.exception.ValidacionDeNegocioException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Eventos por paciente del instrumento: las hojas TPR_, Ausentismo_ y
 * Consultas_ de cada etapa (pretest de las fichas manuales y postest
 * exportado del servidor de la fundacion). Se guardan con el codigo anonimo
 * del paciente y su fuente; no crean pacientes, usuarios ni eventos del
 * sistema, y no alteran lo que mide esta instalacion.
 */
@Service
@RequiredArgsConstructor
public class EventosImportadosUseCase {

    public static final String FUENTE_PRETEST = "Pretest: ficha aplicada a mano y validada por la fundación";
    public static final String FUENTE_POSTEST = "Postest: registro del servidor de la fundación, importado";

    /** Hoja del instrumento -> (fase, tipo de evento). */
    public static final Map<String, Object[]> HOJAS = Map.of(
            "TPR_PRE", new Object[] {Fase.PRETEST, TipoEventoImportado.REGISTRO},
            "TPR_POST", new Object[] {Fase.POSTEST, TipoEventoImportado.REGISTRO},
            "Ausentismo_PRE", new Object[] {Fase.PRETEST, TipoEventoImportado.CITA},
            "Ausentismo_POST", new Object[] {Fase.POSTEST, TipoEventoImportado.CITA},
            "Consultas_PRE", new Object[] {Fase.PRETEST, TipoEventoImportado.CONSULTA},
            "Consultas_POST", new Object[] {Fase.POSTEST, TipoEventoImportado.CONSULTA});

    private final EventosImportadosRepositoryPort repositorio;
    private final ApplicationEventPublisher eventPublisher;

    public record Resultado(Map<String, Integer> eventosPorHoja, int total) {
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'INVESTIGADOR', 'MEDICO', 'RECEPCIONISTA')")
    @Transactional(readOnly = true)
    public List<EventoImportado> listar(TipoEventoImportado tipo, Fase fase) {
        return repositorio.listar(tipo, fase);
    }

    /** @param hojas filas de cada hoja del instrumento (encabezados ya normalizados) */
    @PreAuthorize("hasAnyRole('ADMIN', 'INVESTIGADOR')")
    @Transactional
    public Resultado cargar(Map<String, List<Map<String, String>>> hojas, Long usuarioId, String ipOrigen) {
        List<EventoImportado> eventos = new ArrayList<>();
        Map<String, Integer> porHoja = new java.util.TreeMap<>();
        for (var entrada : HOJAS.entrySet()) {
            List<Map<String, String>> filas = hojas.get(entrada.getKey());
            if (filas == null) {
                continue;
            }
            Fase fase = (Fase) entrada.getValue()[0];
            TipoEventoImportado tipo = (TipoEventoImportado) entrada.getValue()[1];
            int antes = eventos.size();
            int numeroFila = 1;
            for (Map<String, String> fila : filas) {
                numeroFila++;
                String id = primero(fila, "id_registro", "id_cita", "id_consulta");
                if (!id.matches("\\d+")) {
                    continue; // fila de ejemplo ("Ej."), vacia o nota al pie
                }
                try {
                    eventos.add(evento(fila, fase, tipo, Integer.parseInt(id)));
                } catch (RuntimeException e) {
                    throw new ValidacionDeNegocioException("Hoja " + entrada.getKey() + ", fila " + numeroFila + ": " + e.getMessage());
                }
            }
            porHoja.put(entrada.getKey(), eventos.size() - antes);
        }
        if (eventos.isEmpty()) {
            throw new ValidacionDeNegocioException(
                    "El archivo no tiene las hojas TPR_PRE/POST, Ausentismo_PRE/POST ni Consultas_PRE/POST del instrumento");
        }
        repositorio.reemplazar(eventos, usuarioId);
        eventPublisher.publishEvent(OperacionAuditadaEvent.exito(usuarioId, "ESTUDIO_EVENTOS_IMPORTADOS", "ESTUDIO", null,
                null, "eventos=" + eventos.size() + ";" + porHoja, ipOrigen));
        return new Resultado(porHoja, eventos.size());
    }

    private static EventoImportado evento(Map<String, String> f, Fase fase, TipoEventoImportado tipo, int numero) {
        LocalDate fecha = LocalDate.parse(obligatorio(f, "fecha"));
        String codigo = obligatorio(f, "codigo_paciente");
        String fuente = fase == Fase.PRETEST ? FUENTE_PRETEST : FUENTE_POSTEST;
        String control = texto(f, "control_de_calidad");
        return switch (tipo) {
            case REGISTRO -> new EventoImportado(fase, tipo, numero, fecha, codigo, hora(texto(f, "hora_inicio_registro")),
                    hora(texto(f, "hora_fin_registro")), numero(texto(f, "tpr")), texto(f, "modalidad"),
                    null, null, null, null, null, null, null, null, control, fuente);
            case CITA -> new EventoImportado(fase, tipo, numero, fecha, codigo, null, null, null, null,
                    texto(f, "estado"), texto(f, "motivo"), siNo(texto(f, "recordatorio_enviado")),
                    null, null, null, null, null, control, fuente);
            case CONSULTA -> new EventoImportado(fase, tipo, numero, fecha, codigo, null, null, null, null, null, null, null,
                    texto(f, "categoria"), texto(f, "canal"), siNo(texto(f, "resuelta_en_el_primer_contacto")),
                    siNo(texto(f, "derivada_a_otra_persona_o_area")), numero(texto(f, "tiempo_de_respuesta")), control, fuente);
        };
    }

    private static String primero(Map<String, String> fila, String... columnas) {
        for (String c : columnas) {
            String v = texto(fila, c);
            if (!v.isEmpty()) {
                return v;
            }
        }
        return "";
    }

    private static String texto(Map<String, String> fila, String columna) {
        String v = fila.get(columna);
        return v == null ? "" : v.strip();
    }

    private static String obligatorio(Map<String, String> fila, String columna) {
        String v = texto(fila, columna);
        if (v.isEmpty()) {
            throw new ValidacionDeNegocioException("falta '" + columna + "'");
        }
        return v;
    }

    private static LocalTime hora(String valor) {
        if (valor.isEmpty()) {
            return null;
        }
        try {
            return LocalTime.parse(valor.length() == 4 ? "0" + valor : valor);
        } catch (DateTimeParseException e) {
            throw new ValidacionDeNegocioException("hora invalida '" + valor + "'");
        }
    }

    private static Double numero(String valor) {
        if (valor.isEmpty()) {
            return null;
        }
        return Math.round(Double.parseDouble(valor.replace(',', '.')) * 100) / 100.0;
    }

    private static Boolean siNo(String valor) {
        String v = valor.toLowerCase(Locale.ROOT);
        if (v.isEmpty()) {
            return null;
        }
        return v.startsWith("s") || v.equals("true");
    }
}

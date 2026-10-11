package com.threepartners.oncologia.application.dashboard;

import com.threepartners.oncologia.domain.dashboard.ActividadPeriodo;
import com.threepartners.oncologia.domain.estudio.CanalConsulta;
import com.threepartners.oncologia.domain.estudio.CanalMedicion;
import com.threepartners.oncologia.domain.estudio.CategoriaConsulta;
import com.threepartners.oncologia.domain.estudio.EventoImportado;
import com.threepartners.oncologia.domain.estudio.Fase;
import com.threepartners.oncologia.domain.estudio.ResultadoConsulta;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

/**
 * Traduce los eventos importados del instrumento (servidor de la fundacion y
 * fichas del pretest) a las filas del panel, para calcular los indicadores con
 * exactamente las mismas reglas que se aplican a lo que mide esta instalacion.
 * El "paciente" de cada fila es su codigo anonimo.
 */
final class ActividadDesdeEventosImportados {

    private ActividadDesdeEventosImportados() {
    }

    static ActividadPeriodo.Filas filas(List<EventoImportado> eventos) {
        var registros = eventos.stream().filter(e -> e.tipo().name().equals("REGISTRO")).map(e ->
                new ActividadPeriodo.Registro(e.codigoPaciente(), null, e.fecha(), e.horaInicio(),
                        e.minutos() == null ? 0 : e.minutos(),
                        e.fase() == Fase.PRETEST ? CanalMedicion.MANUAL : CanalMedicion.INTRANET,
                        e.controlCalidad() != null && !e.controlCalidad().isBlank() && !e.controlCalidad().equalsIgnoreCase("OK")))
                .toList();
        var citasImportadas = eventos.stream().filter(e -> e.tipo().name().equals("CITA")).toList();
        var citas = citasImportadas.stream().map(e -> new ActividadPeriodo.Cita(e.codigoPaciente(), null, e.fecha(), null,
                estadoCita(e.estado()), Boolean.TRUE.equals(e.recordatorio()), 0)).toList();
        // El instrumento solo dice si la cita tuvo recordatorio (sin canal ni hora de envio).
        var recordatorios = citasImportadas.stream().filter(e -> Boolean.TRUE.equals(e.recordatorio())).map(e ->
                new ActividadPeriodo.Recordatorio(e.codigoPaciente(), e.fecha(), "T24H", "NO_INFORMADO", "ENVIADO",
                        null, null, null)).toList();
        var consultas = eventos.stream().filter(e -> e.tipo().name().equals("CONSULTA")).map(e -> {
            CanalConsulta canal = canal(e.canal(), e.fase());
            boolean derivada = Boolean.TRUE.equals(e.derivada());
            boolean resuelta = Boolean.TRUE.equals(e.resuelta());
            ResultadoConsulta resultado = resuelta
                    ? (canal.esAutomatizado() ? ResultadoConsulta.RESUELTA_BOT : ResultadoConsulta.RESUELTA_PERSONAL)
                    : derivada ? ResultadoConsulta.RESUELTA_PERSONAL : ResultadoConsulta.NO_RESUELTA;
            return new ActividadPeriodo.Consulta(e.codigoPaciente(), e.fecha(), null, categoria(e.categoria()), canal,
                    resultado, derivada, false, e.tiempoRespuesta());
        }).toList();
        return new ActividadPeriodo.Filas(registros, citas, recordatorios, consultas);
    }

    private static String normalizar(String texto) {
        String sinTildes = Normalizer.normalize(texto == null ? "" : texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTildes.strip().toUpperCase(Locale.ROOT).replaceAll("[\\s\\-]+", "_");
    }

    private static String estadoCita(String estado) {
        String e = normalizar(estado);
        if (e.startsWith("NO_ASIST")) return "NO_ASISTIO";
        if (e.startsWith("ASIST")) return "ATENDIDA";
        if (e.startsWith("CANCEL")) return "CANCELADA";
        if (e.startsWith("REPROGRAM")) return "REPROGRAMADA";
        return e.isEmpty() ? "PROGRAMADA" : e;
    }

    private static CategoriaConsulta categoria(String categoria) {
        return CategoriaConsulta.desde(normalizar(categoria).isEmpty() ? null : normalizar(categoria));
    }

    private static CanalConsulta canal(String canal, Fase fase) {
        String c = normalizar(canal);
        if (c.startsWith("CHAT")) return CanalConsulta.CHATBOT_WEB;
        if (c.startsWith("TELEGRAM")) return CanalConsulta.TELEGRAM;
        if (c.startsWith("WHATSAPP")) return CanalConsulta.WHATSAPP;
        if (c.startsWith("TELEF") || c.startsWith("LLAMADA")) return CanalConsulta.LLAMADA;
        if (c.startsWith("PRESENCIAL")) return CanalConsulta.PRESENCIAL;
        return fase == Fase.POSTEST ? CanalConsulta.CHATBOT_WEB : CanalConsulta.PRESENCIAL;
    }
}

package com.threepartners.oncologia.domain.dashboard;

import com.threepartners.oncologia.domain.estudio.CanalConsulta;
import com.threepartners.oncologia.domain.estudio.CanalMedicion;
import com.threepartners.oncologia.domain.estudio.CategoriaConsulta;
import com.threepartners.oncologia.domain.estudio.RecoleccionSesiones;
import com.threepartners.oncologia.domain.estudio.ResultadoConsulta;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Todo lo que paso en el sistema en un periodo, para el panel del personal:
 * registros de pacientes (tiempo de registro), citas (ausentismo),
 * recordatorios enviados y consultas (atendidas por el asistente). Las filas
 * llevan nombres, no ids: las lee el personal, que ya tiene acceso a esos
 * pacientes.
 *
 * Los indicadores se calculan con las mismas reglas que la recoleccion por
 * sesion (RecoleccionSesiones), para que el panel y lo reportado coincidan.
 */
public final class ActividadPeriodo {

    private ActividadPeriodo() {
    }

    public record Registro(String paciente, String registradoPor, LocalDate fecha, LocalTime hora,
                           double minutos, CanalMedicion canal, boolean sospechosa) {
    }

    public record Cita(String paciente, String medico, LocalDate fecha, LocalTime hora, String estado,
                       boolean recordatorioEnviado, int vecesReprogramada) {

        RecoleccionSesiones.FilaCita comoFila() {
            return new RecoleccionSesiones.FilaCita(paciente, fecha, hora, estado, recordatorioEnviado, vecesReprogramada);
        }
    }

    /** destinatario: PACIENTE o REFERIDO; enviadoEn null si no se envio. */
    public record Recordatorio(String paciente, LocalDate fechaCita, String tipo, String canal, String estado,
                               LocalDateTime programadoPara, LocalDateTime enviadoEn, String respuesta) {

        public boolean enviado() {
            return "ENVIADO".equals(estado);
        }
    }

    /** paciente null = visitante sin identificar (chat publico). */
    public record Consulta(String paciente, LocalDate fecha, LocalTime hora, CategoriaConsulta categoria,
                           CanalConsulta canal, ResultadoConsulta resultado, boolean derivada, boolean reabierta,
                           Double tiempoRespuestaMin) {

        RecoleccionSesiones.FilaConsulta comoFila() {
            return new RecoleccionSesiones.FilaConsulta(paciente, fecha, hora, categoria, canal, resultado,
                    derivada, reabierta, tiempoRespuestaMin);
        }

        public boolean abierta() {
            return resultado == null;
        }

        public boolean resueltaPorAsistente() {
            return comoFila().resueltaEnPrimerContacto();
        }

        public boolean derivadaAlPersonal() {
            return comoFila().derivadaAlPersonal();
        }
    }

    public record Filas(List<Registro> registros, List<Cita> citas, List<Recordatorio> recordatorios,
                        List<Consulta> consultas) {
    }

    public record TotalRegistros(int total, Double promedioMin, Double minimoMin, Double maximoMin, int porRevisar) {
    }

    public record TotalCitas(int total, int atendidas, int noAsistio, int canceladas, int pendientes,
                             int sinDesenlace, int reprogramadas, Double ausentismoPct) {
    }

    public record TotalRecordatorios(int enviados, int fallidos, int pendientes, int respondidos,
                                     Map<String, Integer> enviadosPorCanal, int citasConRecordatorio,
                                     Double coberturaPct) {
    }

    public record TotalConsultas(int total, int abiertas, int cerradas, int resueltasPorAsistente,
                                 int derivadas, Double atendidasAutomaticoPct, Double tiempoRespuestaPromedioMin,
                                 Map<String, Integer> porCategoria) {
    }

    /** Una fila por dia con actividad. Los valores null = ese dia no hubo eventos del indicador. */
    public record Dia(LocalDate fecha, int registros, Double tprMin, int citasElegibles, int inasistencias,
                      Double ausentismoPct, int recordatoriosEnviados, int consultas, int resueltasPorAsistente,
                      Double atendidasAutomaticoPct) {
    }

    public record Resumen(LocalDate desde, LocalDate hasta, TotalRegistros registros, TotalCitas citas,
                          TotalRecordatorios recordatorios, TotalConsultas consultas, List<Dia> dias) {
    }

    public static Resumen resumir(LocalDate desde, LocalDate hasta, Filas filas, LocalDate hoy) {
        return new Resumen(desde, hasta,
                registros(filas.registros()),
                citas(filas.citas(), hoy),
                recordatorios(filas.recordatorios(), filas.citas()),
                consultas(filas.consultas()),
                dias(filas));
    }

    private static TotalRegistros registros(List<Registro> registros) {
        var minutos = registros.stream().mapToDouble(Registro::minutos).summaryStatistics();
        boolean hay = !registros.isEmpty();
        return new TotalRegistros(registros.size(),
                hay ? redondear(minutos.getAverage()) : null,
                hay ? redondear(minutos.getMin()) : null,
                hay ? redondear(minutos.getMax()) : null,
                (int) registros.stream().filter(Registro::sospechosa).count());
    }

    private static TotalCitas citas(List<Cita> citas, LocalDate hoy) {
        var filas = citas.stream().map(Cita::comoFila).toList();
        int atendidas = contar(citas, c -> "ATENDIDA".equals(c.estado()));
        int noAsistio = contar(citas, c -> "NO_ASISTIO".equals(c.estado()));
        int elegibles = atendidas + noAsistio;
        return new TotalCitas(citas.size(), atendidas, noAsistio,
                contar(citas, c -> "CANCELADA".equals(c.estado())),
                contar(citas, c -> "PROGRAMADA".equals(c.estado()) || "CONFIRMADA".equals(c.estado())),
                (int) filas.stream().filter(f -> f.sinDesenlace(hoy)).count(),
                contar(citas, c -> c.vecesReprogramada() > 0),
                pct(noAsistio, elegibles));
    }

    private static TotalRecordatorios recordatorios(List<Recordatorio> recordatorios, List<Cita> citas) {
        Map<String, Integer> porCanal = new TreeMap<>(recordatorios.stream().filter(Recordatorio::enviado)
                .collect(Collectors.groupingBy(Recordatorio::canal, Collectors.summingInt(r -> 1))));
        var elegibles = citas.stream().map(Cita::comoFila).filter(RecoleccionSesiones.FilaCita::elegible).toList();
        int conRecordatorio = (int) elegibles.stream().filter(RecoleccionSesiones.FilaCita::recordatorioEnviado).count();
        return new TotalRecordatorios(
                contar(recordatorios, Recordatorio::enviado),
                contar(recordatorios, r -> "FALLIDO".equals(r.estado())),
                contar(recordatorios, r -> "PENDIENTE".equals(r.estado()) || "EN_PROCESO".equals(r.estado())),
                contar(recordatorios, r -> r.respuesta() != null),
                porCanal, conRecordatorio, pct(conRecordatorio, elegibles.size()));
    }

    private static TotalConsultas consultas(List<Consulta> consultas) {
        var cerradas = consultas.stream().filter(k -> !k.abierta()).toList();
        int resueltas = contar(cerradas, Consulta::resueltaPorAsistente);
        var tiempos = consultas.stream().map(Consulta::tiempoRespuestaMin).filter(t -> t != null)
                .mapToDouble(Double::doubleValue).summaryStatistics();
        Map<String, Integer> porCategoria = new TreeMap<>(consultas.stream()
                .collect(Collectors.groupingBy(k -> k.categoria() == null ? "OTRO" : k.categoria().name(),
                        Collectors.summingInt(k -> 1))));
        return new TotalConsultas(consultas.size(), consultas.size() - cerradas.size(), cerradas.size(), resueltas,
                contar(consultas, Consulta::derivadaAlPersonal),
                pct(resueltas, cerradas.size()),
                tiempos.getCount() == 0 ? null : redondear(tiempos.getAverage()),
                porCategoria);
    }

    private static List<Dia> dias(Filas filas) {
        var fechas = new TreeSet<LocalDate>();
        filas.registros().forEach(r -> fechas.add(r.fecha()));
        filas.citas().stream().filter(c -> c.comoFila().elegible()).forEach(c -> fechas.add(c.fecha()));
        filas.recordatorios().stream().filter(r -> r.enviadoEn() != null).forEach(r -> fechas.add(r.enviadoEn().toLocalDate()));
        filas.consultas().forEach(k -> fechas.add(k.fecha()));

        List<Dia> dias = new ArrayList<>();
        for (LocalDate fecha : fechas) {
            var registros = delDia(filas.registros(), Registro::fecha, fecha);
            var citas = delDia(filas.citas(), Cita::fecha, fecha);
            var cerradas = delDia(filas.consultas(), Consulta::fecha, fecha).stream().filter(k -> !k.abierta()).toList();
            int elegibles = contar(citas, c -> c.comoFila().elegible());
            int inasistencias = contar(citas, c -> "NO_ASISTIO".equals(c.estado()));
            int resueltas = contar(cerradas, Consulta::resueltaPorAsistente);
            dias.add(new Dia(fecha, registros.size(),
                    registros.isEmpty() ? null : redondear(registros.stream().mapToDouble(Registro::minutos).average().orElse(0)),
                    elegibles, inasistencias, pct(inasistencias, elegibles),
                    contar(filas.recordatorios(), r -> r.enviadoEn() != null && fecha.equals(r.enviadoEn().toLocalDate())),
                    cerradas.size(), resueltas, pct(resueltas, cerradas.size())));
        }
        return dias;
    }

    private static <T> List<T> delDia(List<T> filas, Function<T, LocalDate> fecha, LocalDate dia) {
        return filas.stream().filter(f -> dia.equals(fecha.apply(f))).toList();
    }

    private static <T> int contar(List<T> filas, java.util.function.Predicate<T> condicion) {
        return (int) filas.stream().filter(condicion).count();
    }

    private static Double pct(int parte, int total) {
        return total == 0 ? null : redondear(100.0 * parte / total);
    }

    private static Double redondear(double valor) {
        return BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}

package com.threepartners.oncologia.domain.estudio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Recoleccion por sesion de la tesis v8: 13 sesiones (lunes, miercoles y
 * viernes) por etapa, tratadas como grupos independientes. Cada sesion da un
 * valor por indicador, que es lo que se analiza en SPSS (t de Student o U de
 * Mann-Whitney).
 *
 * Definiciones operacionales (capitulo de Metodologia y Anexo 2):
 * - TPR: alta de paciente hecha por el personal; inicio = se abre el
 *   formulario, fin = se guarda (marcas del servidor). Minutos.
 * - TA: no asistidas sin aviso / citas elegibles x 100. Elegibles = con
 *   desenlace (asistio o no asistio); las canceladas no cuentan, y una cita
 *   reprogramada cuenta una sola vez, en su fecha final.
 * - NCA: resueltas en el primer contacto y sin derivacion / consultas
 *   recibidas x 100. Las abiertas aun no tienen desenlace y se avisan; los
 *   mensajes fuera de alcance nunca se registran como consulta.
 *
 * Aqui no hay acceso a datos: recibe las filas y calcula, de modo que lo que
 * se ve en el panel y lo que se exporta salen del mismo calculo.
 */
public final class RecoleccionSesiones {

    private RecoleccionSesiones() {
    }

    /** Ficha 01 (TPR). */
    public record FilaTiempo(String codigo, LocalDate fecha, LocalTime horaInicio, LocalTime horaFin,
                             double minutos, CanalMedicion canal, boolean sospechosa) {
    }

    /** Ficha 02 (citas). estado es el de la cita en el sistema. */
    public record FilaCita(String codigo, LocalDate fecha, LocalTime hora, String estado,
                           boolean recordatorioEnviado, int vecesReprogramada) {

        public boolean elegible() {
            return "ATENDIDA".equals(estado) || "NO_ASISTIO".equals(estado);
        }

        public boolean inasistencia() {
            return "NO_ASISTIO".equals(estado);
        }

        /** Cita ya pasada que nadie marco como asistida o no asistida. */
        public boolean sinDesenlace(LocalDate hoy) {
            return ("PROGRAMADA".equals(estado) || "CONFIRMADA".equals(estado)) && fecha.isBefore(hoy);
        }

        /** Texto de la ficha de la tesis. */
        public String estadoFicha() {
            return switch (estado) {
                case "ATENDIDA" -> "Asistió";
                case "NO_ASISTIO" -> "No asistió";
                case "CANCELADA" -> "Cancelada";
                default -> "Pendiente";
            };
        }
    }

    /** Ficha 03 (consultas). resultado null = todavia abierta. */
    public record FilaConsulta(String codigo, LocalDate fecha, LocalTime hora, CategoriaConsulta categoria,
                               CanalConsulta canal, ResultadoConsulta resultado, boolean derivada,
                               boolean reabierta, Double tiempoRespuestaMin) {

        public boolean abierta() {
            return resultado == null;
        }

        /** Paso al personal en algun momento (o el personal la resolvio). */
        public boolean derivadaAlPersonal() {
            return derivada || resultado == ResultadoConsulta.ESCALADA
                    || (resultado == ResultadoConsulta.RESUELTA_PERSONAL && canal.esAutomatizado());
        }

        /**
         * Numerador del NCA. En el postest la resuelve el chatbot; en una
         * captura manual (pretest en el sistema) la resuelve el personal que
         * la recibio, sin derivarla.
         */
        public boolean resueltaEnPrimerContacto() {
            if (abierta() || reabierta || derivadaAlPersonal()) {
                return false;
            }
            return resultado == ResultadoConsulta.RESUELTA_BOT
                    || (resultado == ResultadoConsulta.RESUELTA_PERSONAL && !canal.esAutomatizado());
        }
    }

    public record Sesion(int numero, LocalDate fecha, int registros, Double tprMin, int citasElegibles,
                         int inasistencias, Double taPct, int consultas, int resueltas, Double ncaPct) {
    }

    /** Un valor por indicador; null = sin eventos (la tesis excluye esas sesiones del analisis). */
    public record Indicadores(Double tprMin, Double taPct, Double ncaPct) {
    }

    public record Avisos(int citasSinDesenlace, int consultasAbiertas, int registrosSospechosos,
                         int eventosFueraDeSesion) {

        public boolean hayPendientes() {
            return citasSinDesenlace > 0 || consultasAbiertas > 0;
        }
    }

    /**
     * @param citasConRecordatorio citas elegibles que recibieron al menos un
     *                             recordatorio: un recordatorio solo reduce el
     *                             ausentismo si llega, asi que se reporta junto al TA.
     */
    public record Resumen(Fase fase, LocalDate desde, LocalDate hasta, List<DayOfWeek> diasSesion,
                          List<Sesion> sesiones, Indicadores promedioSesiones, Indicadores global,
                          int registros, int citasElegibles, int consultas, Avisos avisos,
                          int citasConRecordatorio, Double coberturaRecordatorioPct) {
    }

    public record Detalle(List<FilaTiempo> tiempos, List<FilaCita> citas, List<FilaConsulta> consultas) {
    }

    /** Fechas de sesion de una fase: los dias configurados dentro de su rango. */
    public static List<LocalDate> fechasDeSesion(LocalDate desde, LocalDate hasta, Set<DayOfWeek> dias) {
        List<LocalDate> fechas = new ArrayList<>();
        for (LocalDate d = desde; !d.isAfter(hasta); d = d.plusDays(1)) {
            if (dias.contains(d.getDayOfWeek())) {
                fechas.add(d);
            }
        }
        return fechas;
    }

    /** Solo las filas de los dias de sesion: lo que entra a las fichas. */
    public static Detalle filtrar(Detalle todo, Set<LocalDate> fechas) {
        return new Detalle(
                todo.tiempos().stream().filter(t -> fechas.contains(t.fecha())).toList(),
                todo.citas().stream().filter(c -> fechas.contains(c.fecha())).toList(),
                todo.consultas().stream().filter(k -> fechas.contains(k.fecha())).toList());
    }

    public static Resumen resumir(Fase fase, LocalDate desde, LocalDate hasta, Set<DayOfWeek> dias,
                                  Detalle todo, LocalDate hoy) {
        List<LocalDate> fechas = fechasDeSesion(desde, hasta, dias);
        Detalle enSesion = filtrar(todo, Set.copyOf(fechas));

        List<Sesion> sesiones = new ArrayList<>();
        int numero = 1;
        for (LocalDate fecha : fechas) {
            sesiones.add(sesion(numero++, fecha, enSesion));
        }

        Indicadores promedio = new Indicadores(
                promedio(sesiones, Sesion::tprMin), promedio(sesiones, Sesion::taPct), promedio(sesiones, Sesion::ncaPct));
        Indicadores global = indicadores(enSesion.tiempos(), enSesion.citas(), enSesion.consultas());

        int fueraDeSesion = (todo.tiempos().size() - enSesion.tiempos().size())
                + (int) (todo.citas().stream().filter(FilaCita::elegible).count()
                - enSesion.citas().stream().filter(FilaCita::elegible).count())
                + (todo.consultas().size() - enSesion.consultas().size());
        Avisos avisos = new Avisos(
                (int) enSesion.citas().stream().filter(c -> c.sinDesenlace(hoy)).count(),
                (int) enSesion.consultas().stream().filter(FilaConsulta::abierta).count(),
                (int) enSesion.tiempos().stream().filter(FilaTiempo::sospechosa).count(),
                fueraDeSesion);

        List<DayOfWeek> diasOrdenados = dias.stream().sorted().toList();
        int elegibles = (int) enSesion.citas().stream().filter(FilaCita::elegible).count();
        int conRecordatorio = (int) enSesion.citas().stream().filter(c -> c.elegible() && c.recordatorioEnviado()).count();
        return new Resumen(fase, desde, hasta, diasOrdenados, sesiones, promedio, global,
                enSesion.tiempos().size(),
                elegibles,
                (int) enSesion.consultas().stream().filter(k -> !k.abierta()).count(),
                avisos,
                conRecordatorio,
                elegibles == 0 ? null : redondear(100.0 * conRecordatorio / elegibles));
    }

    private static Sesion sesion(int numero, LocalDate fecha, Detalle enSesion) {
        List<FilaTiempo> tiempos = delDia(enSesion.tiempos(), FilaTiempo::fecha, fecha);
        List<FilaCita> citas = delDia(enSesion.citas(), FilaCita::fecha, fecha);
        List<FilaConsulta> consultas = delDia(enSesion.consultas(), FilaConsulta::fecha, fecha);
        Indicadores ind = indicadores(tiempos, citas, consultas);
        int elegibles = (int) citas.stream().filter(FilaCita::elegible).count();
        int inasistencias = (int) citas.stream().filter(FilaCita::inasistencia).count();
        List<FilaConsulta> cerradas = consultas.stream().filter(k -> !k.abierta()).toList();
        int resueltas = (int) cerradas.stream().filter(FilaConsulta::resueltaEnPrimerContacto).count();
        return new Sesion(numero, fecha, tiempos.size(), ind.tprMin(), elegibles, inasistencias, ind.taPct(),
                cerradas.size(), resueltas, ind.ncaPct());
    }

    static Indicadores indicadores(List<FilaTiempo> tiempos, List<FilaCita> citas, List<FilaConsulta> consultas) {
        Double tpr = tiempos.isEmpty() ? null
                : redondear(tiempos.stream().mapToDouble(FilaTiempo::minutos).sum() / tiempos.size());
        long elegibles = citas.stream().filter(FilaCita::elegible).count();
        Double ta = elegibles == 0 ? null
                : redondear(100.0 * citas.stream().filter(FilaCita::inasistencia).count() / elegibles);
        List<FilaConsulta> cerradas = consultas.stream().filter(k -> !k.abierta()).toList();
        Double nca = cerradas.isEmpty() ? null
                : redondear(100.0 * cerradas.stream().filter(FilaConsulta::resueltaEnPrimerContacto).count() / cerradas.size());
        return new Indicadores(tpr, ta, nca);
    }

    private static <T> List<T> delDia(List<T> filas, Function<T, LocalDate> fecha, LocalDate dia) {
        Predicate<T> mismoDia = f -> dia.equals(fecha.apply(f));
        return filas.stream().filter(mismoDia).toList();
    }

    /** Promedio de los valores por sesion; las sesiones sin eventos no cuentan. */
    private static Double promedio(List<Sesion> sesiones, Function<Sesion, Double> valor) {
        var conDatos = sesiones.stream().map(valor).filter(v -> v != null).toList();
        return conDatos.isEmpty() ? null
                : redondear(conDatos.stream().mapToDouble(Double::doubleValue).sum() / conDatos.size());
    }

    static Double redondear(double valor) {
        return BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}

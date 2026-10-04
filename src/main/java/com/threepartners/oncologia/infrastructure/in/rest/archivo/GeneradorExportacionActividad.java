package com.threepartners.oncologia.infrastructure.in.rest.archivo;

import com.threepartners.oncologia.application.dashboard.ConsultarActividadUseCase;
import com.threepartners.oncologia.domain.dashboard.ActividadPeriodo;
import com.threepartners.oncologia.domain.estudio.ResultadoConsulta;
import com.threepartners.oncologia.domain.shared.ZonaHoraria;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Libro del panel: resumen del periodo, una fila por dia y el detalle de
 * registros, citas, recordatorios y consultas, con nombres (como en pantalla).
 */
@Component
public class GeneradorExportacionActividad {

    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final Map<String, String> ESTADOS_CITA = Map.of(
            "PROGRAMADA", "Programada", "CONFIRMADA", "Confirmada", "ATENDIDA", "Asistió",
            "NO_ASISTIO", "No asistió", "CANCELADA", "Cancelada");

    private static final Map<String, String> CANALES_RECORDATORIO = Map.of(
            "TELEGRAM", "Telegram (paciente)", "TELEGRAM_REFERIDO", "Telegram (acompañante)",
            "LLAMADA", "Llamada", "CORREO", "Correo");

    public byte[] generar(ConsultarActividadUseCase.Exportacion datos) {
        try (var libro = new XSSFWorkbook()) {
            HojaExcel h = new HojaExcel(libro);
            resumen(h, datos);
            dias(h, datos.resumen().dias());
            registros(h, datos.filas().registros());
            citas(h, datos.filas().citas());
            recordatorios(h, datos.filas().recordatorios());
            consultas(h, datos.filas().consultas());
            return h.aBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void resumen(HojaExcel h, ConsultarActividadUseCase.Exportacion datos) {
        var r = datos.resumen();
        Sheet hoja = h.hojaConEncabezados("Resumen", List.of("Indicador", "Valor", "Detalle"));
        hoja.setColumnWidth(0, 44 * 256);
        hoja.setColumnWidth(2, 50 * 256);
        Object[][] filas = {
                {"Periodo", null, "Del " + r.desde() + " al " + r.hasta()},
                {"Pacientes registrados por el personal", (double) r.registros().total(), null},
                {"Tiempo promedio de registro (min)", r.registros().promedioMin(),
                        "Mínimo " + texto(r.registros().minimoMin()) + " · máximo " + texto(r.registros().maximoMin())},
                {"Citas en el periodo", (double) r.citas().total(), r.citas().atendidas() + " asistieron · "
                        + r.citas().noAsistio() + " no asistieron · " + r.citas().canceladas() + " canceladas"},
                {"Ausentismo (%)", r.citas().ausentismoPct(), "No asistió / (asistió + no asistió)"},
                {"Recordatorios enviados", (double) r.recordatorios().enviados(),
                        r.recordatorios().fallidos() + " fallidos · " + r.recordatorios().respondidos() + " respondidos"},
                {"Citas con recordatorio (%)", r.recordatorios().coberturaPct(), null},
                {"Consultas recibidas", (double) r.consultas().total(), r.consultas().abiertas() + " abiertas"},
                {"Resueltas por el asistente", (double) r.consultas().resueltasPorAsistente(),
                        r.consultas().derivadas() + " derivadas al personal"},
                {"Consultas atendidas automáticamente (%)", r.consultas().atendidasAutomaticoPct(),
                        "Resueltas al primer contacto, sin derivar / consultas cerradas"},
        };
        int i = 1;
        for (Object[] f : filas) {
            Row fila = hoja.createRow(i++);
            h.negrita(fila, 0, (String) f[0]);
            h.decimal(fila, 1, (Double) f[1]);
            h.texto(fila, 2, (String) f[2]);
        }
        Row pie = hoja.createRow(i + 1);
        h.texto(pie, 0, "Generado el " + datos.generadoEn().atZone(ZonaHoraria.LIMA).format(FECHA_HORA));
    }

    private static void dias(HojaExcel h, List<ActividadPeriodo.Dia> dias) {
        Sheet hoja = h.hojaConEncabezados("Por día", List.of("Fecha", "Registros", "Tiempo registro (min)",
                "Citas con desenlace", "No asistieron", "Ausentismo (%)", "Recordatorios enviados",
                "Consultas cerradas", "Resueltas por el asistente", "Atendidas automáticamente (%)"));
        int i = 1;
        for (var d : dias) {
            Row fila = hoja.createRow(i++);
            h.fecha(fila, 0, d.fecha());
            h.entero(fila, 1, d.registros());
            h.decimal(fila, 2, d.tprMin());
            h.entero(fila, 3, d.citasElegibles());
            h.entero(fila, 4, d.inasistencias());
            h.decimal(fila, 5, d.ausentismoPct());
            h.entero(fila, 6, d.recordatoriosEnviados());
            h.entero(fila, 7, d.consultas());
            h.entero(fila, 8, d.resueltasPorAsistente());
            h.decimal(fila, 9, d.atendidasAutomaticoPct());
        }
    }

    private static void registros(HojaExcel h, List<ActividadPeriodo.Registro> registros) {
        Sheet hoja = h.hojaConEncabezados("Registros", List.of("Fecha", "Hora", "Paciente", "Registrado por",
                "Tiempo (min)", "Revisar"));
        hoja.setColumnWidth(2, 32 * 256);
        int i = 1;
        for (var r : registros) {
            Row fila = hoja.createRow(i++);
            h.fecha(fila, 0, r.fecha());
            h.hora(fila, 1, r.hora());
            h.texto(fila, 2, r.paciente());
            h.texto(fila, 3, r.registradoPor());
            h.decimal(fila, 4, Math.round(r.minutos() * 100) / 100.0);
            h.texto(fila, 5, r.sospechosa() ? "Sí" : "No");
        }
    }

    private static void citas(HojaExcel h, List<ActividadPeriodo.Cita> citas) {
        Sheet hoja = h.hojaConEncabezados("Citas", List.of("Fecha", "Hora", "Paciente", "Médico", "Estado",
                "Recordatorio enviado", "Veces reprogramada"));
        hoja.setColumnWidth(2, 32 * 256);
        int i = 1;
        for (var c : citas) {
            Row fila = hoja.createRow(i++);
            h.fecha(fila, 0, c.fecha());
            h.hora(fila, 1, c.hora());
            h.texto(fila, 2, c.paciente());
            h.texto(fila, 3, c.medico() != null ? c.medico() : "Sin asignar");
            h.texto(fila, 4, ESTADOS_CITA.getOrDefault(c.estado(), c.estado()));
            h.texto(fila, 5, c.recordatorioEnviado() ? "Sí" : "No");
            h.entero(fila, 6, c.vecesReprogramada());
        }
    }

    private static void recordatorios(HojaExcel h, List<ActividadPeriodo.Recordatorio> recordatorios) {
        Sheet hoja = h.hojaConEncabezados("Recordatorios", List.of("Paciente", "Fecha de la cita", "Canal", "Estado",
                "Programado para", "Enviado", "Respuesta"));
        hoja.setColumnWidth(0, 32 * 256);
        int i = 1;
        for (var r : recordatorios) {
            Row fila = hoja.createRow(i++);
            h.texto(fila, 0, r.paciente());
            h.fecha(fila, 1, r.fechaCita());
            h.texto(fila, 2, CANALES_RECORDATORIO.getOrDefault(r.canal(), r.canal()));
            h.texto(fila, 3, etiqueta(r.estado()));
            h.texto(fila, 4, r.programadoPara() != null ? r.programadoPara().format(FECHA_HORA) : null);
            h.texto(fila, 5, r.enviadoEn() != null ? r.enviadoEn().format(FECHA_HORA) : null);
            h.texto(fila, 6, r.respuesta());
        }
    }

    private static void consultas(HojaExcel h, List<ActividadPeriodo.Consulta> consultas) {
        Sheet hoja = h.hojaConEncabezados("Consultas", List.of("Fecha", "Hora", "Paciente", "Canal", "Tema",
                "Resultado", "Resuelta por el asistente", "Tiempo de respuesta (min)"));
        hoja.setColumnWidth(2, 32 * 256);
        int i = 1;
        for (var k : consultas) {
            Row fila = hoja.createRow(i++);
            h.fecha(fila, 0, k.fecha());
            h.hora(fila, 1, k.hora());
            h.texto(fila, 2, k.paciente() != null ? k.paciente() : "Visitante sin identificar");
            h.texto(fila, 3, etiqueta(k.canal().name()));
            h.texto(fila, 4, k.categoria() != null ? etiqueta(k.categoria().name()) : null);
            h.texto(fila, 5, resultado(k));
            h.texto(fila, 6, k.resueltaPorAsistente() ? "Sí" : "No");
            h.decimal(fila, 7, k.tiempoRespuestaMin() != null ? Math.round(k.tiempoRespuestaMin() * 100) / 100.0 : null);
        }
    }

    private static String resultado(ActividadPeriodo.Consulta k) {
        if (k.abierta()) {
            return "Abierta";
        }
        if (k.derivadaAlPersonal() || k.resultado() == ResultadoConsulta.ESCALADA) {
            return "Derivada al personal";
        }
        return k.resultado() == ResultadoConsulta.RESUELTA_BOT ? "Resuelta por el asistente" : "Resuelta por el personal";
    }

    /** INFORMACION_INSTITUCIONAL -> "Informacion institucional". */
    private static String etiqueta(String valor) {
        String texto = valor.replace('_', ' ').toLowerCase(java.util.Locale.ROOT);
        return Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }

    private static String texto(Double valor) {
        return valor == null ? "—" : String.valueOf(valor);
    }
}

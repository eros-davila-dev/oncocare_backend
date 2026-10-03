package com.threepartners.oncologia.infrastructure.in.rest.archivo;

import com.threepartners.oncologia.application.estudio.RecoleccionPorSesionUseCase;
import com.threepartners.oncologia.domain.estudio.CategoriaConsulta;
import com.threepartners.oncologia.domain.estudio.Fase;
import com.threepartners.oncologia.domain.estudio.RecoleccionSesiones;
import com.threepartners.oncologia.domain.shared.ZonaHoraria;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Libro de la recoleccion por sesion con las MISMAS hojas y columnas del
 * Instrumento de la tesis (TPR_POST, Ausentismo_POST, Consultas_POST,
 * Sesiones y SPSS_Independientes): se pega en el Instrumento y la sintaxis
 * Analisis_SPSS_opcionB.sps corre sin cambios.
 *
 * Porcentajes en 0-100 con dos decimales (como la hoja SPSS_Independientes);
 * una sesion sin eventos deja la celda vacia (valor perdido en SPSS).
 */
@Component
public class GeneradorExportacionRecoleccion {

    private static final DateTimeFormatter FECHA_TEXTO = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final Map<CategoriaConsulta, String> CATEGORIAS = Map.of(
            CategoriaConsulta.CITAS, "Citas",
            CategoriaConsulta.HORARIOS, "Horarios",
            CategoriaConsulta.INFORMACION_INSTITUCIONAL, "Información institucional",
            CategoriaConsulta.REQUISITOS, "Requisitos",
            CategoriaConsulta.UBICACION, "Ubicación",
            CategoriaConsulta.SEGUIMIENTO_ADMINISTRATIVO, "Seguimiento administrativo",
            CategoriaConsulta.OTRO, "Otro");

    public byte[] generar(RecoleccionPorSesionUseCase.Exportacion datos) {
        try (var libro = new XSSFWorkbook()) {
            HojaExcel hojas = new HojaExcel(libro);
            Fase fase = datos.resumen().fase();
            String sufijo = fase == Fase.PRETEST ? "PRE" : "POST";
            String etapa = fase == Fase.PRETEST ? "Pretest" : "Postest";

            tiempos(hojas, "TPR_" + sufijo, datos.detalle().tiempos());
            citas(hojas, "Ausentismo_" + sufijo, datos.detalle().citas());
            consultas(hojas, "Consultas_" + sufijo, datos.detalle().consultas());
            sesiones(hojas, datos.resumen());
            spss(hojas, datos.resumen(), etapa, fase == Fase.PRETEST ? 1 : 2);
            metadatos(hojas, datos);
            return hojas.aBytes();
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }

    private static void tiempos(HojaExcel h, String nombre, List<RecoleccionSesiones.FilaTiempo> filas) {
        Sheet hoja = h.hojaConEncabezados(nombre, List.of("ID registro", "Fecha", "Código paciente",
                "Hora inicio registro", "Hora fin registro", "TPR (min)", "Modalidad", "Control de calidad"));
        int r = 1;
        for (var f : filas) {
            Row fila = hoja.createRow(r);
            h.entero(fila, 0, r);
            h.fecha(fila, 1, f.fecha());
            h.texto(fila, 2, f.codigo());
            h.hora(fila, 3, f.horaInicio());
            h.hora(fila, 4, f.horaFin());
            h.decimal(fila, 5, RecoleccionRedondeo.dos(f.minutos()));
            h.texto(fila, 6, "Sistema");
            h.texto(fila, 7, f.sospechosa() ? "Revisar" : "OK");
            r++;
        }
    }

    private static void citas(HojaExcel h, String nombre, List<RecoleccionSesiones.FilaCita> filas) {
        Sheet hoja = h.hojaConEncabezados(nombre, List.of("ID cita", "Fecha", "Código paciente", "Cita programada",
                "Estado", "Motivo (si no asistió)", "Recordatorio enviado", "Elegible", "Veces reprogramada"));
        int r = 1;
        for (var f : filas) {
            Row fila = hoja.createRow(r);
            h.entero(fila, 0, r);
            h.fecha(fila, 1, f.fecha());
            h.texto(fila, 2, f.codigo());
            h.texto(fila, 3, "Sí");
            h.texto(fila, 4, f.estadoFicha());
            h.texto(fila, 5, f.inasistencia() ? "Sin aviso" : null);
            h.texto(fila, 6, f.recordatorioEnviado() ? "Sí" : "No");
            h.texto(fila, 7, f.elegible() ? "Sí" : "No");
            h.entero(fila, 8, f.vecesReprogramada());
            r++;
        }
    }

    private static void consultas(HojaExcel h, String nombre, List<RecoleccionSesiones.FilaConsulta> filas) {
        Sheet hoja = h.hojaConEncabezados(nombre, List.of("ID consulta", "Fecha", "Código paciente", "Categoría",
                "Canal", "Resuelta en el primer contacto", "Derivada a otra persona o área", "Tiempo de respuesta (min)",
                "Estado en el sistema"));
        int r = 1;
        for (var f : filas) {
            Row fila = hoja.createRow(r);
            h.entero(fila, 0, r);
            h.fecha(fila, 1, f.fecha());
            h.texto(fila, 2, f.codigo());
            h.texto(fila, 3, f.categoria() != null ? CATEGORIAS.get(f.categoria()) : "Sin categoría");
            h.texto(fila, 4, switch (f.canal()) {
                case CHATBOT_WEB -> "Chatbot";
                case TELEGRAM -> "Telegram";
                case WHATSAPP -> "WhatsApp institucional";
                case LLAMADA -> "Telefónico";
                case PRESENCIAL -> "Presencial";
            });
            h.texto(fila, 5, f.abierta() ? null : f.resueltaEnPrimerContacto() ? "Sí" : "No");
            h.texto(fila, 6, f.derivadaAlPersonal() ? "Sí" : "No");
            h.decimal(fila, 7, f.tiempoRespuestaMin() != null ? RecoleccionRedondeo.dos(f.tiempoRespuestaMin()) : null);
            h.texto(fila, 8, f.abierta() ? "ABIERTA (pendiente)" : f.resultado().name());
            r++;
        }
    }

    private static void sesiones(HojaExcel h, RecoleccionSesiones.Resumen resumen) {
        Sheet hoja = h.hojaConEncabezados("Sesiones", List.of("Sesión", "Fecha", "Día", "Registros", "TPR (min)",
                "Citas elegibles", "Inasistencias", "Ausentismo (%)", "Consultas", "Resueltas primer contacto",
                "Consultas atendidas (%)"));
        int r = 1;
        for (var s : resumen.sesiones()) {
            Row fila = hoja.createRow(r++);
            h.entero(fila, 0, s.numero());
            h.fecha(fila, 1, s.fecha());
            h.texto(fila, 2, s.fecha().getDayOfWeek().getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.forLanguageTag("es-PE")));
            h.entero(fila, 3, s.registros());
            h.decimal(fila, 4, s.tprMin());
            h.entero(fila, 5, s.citasElegibles());
            h.entero(fila, 6, s.inasistencias());
            h.decimal(fila, 7, s.taPct());
            h.entero(fila, 8, s.consultas());
            h.entero(fila, 9, s.resueltas());
            h.decimal(fila, 10, s.ncaPct());
        }
        r++;
        Row promedio = hoja.createRow(r++);
        h.negrita(promedio, 0, "Promedio de sesiones");
        h.decimal(promedio, 4, resumen.promedioSesiones().tprMin());
        h.decimal(promedio, 7, resumen.promedioSesiones().taPct());
        h.decimal(promedio, 10, resumen.promedioSesiones().ncaPct());
        Row global = hoja.createRow(r);
        h.negrita(global, 0, "Global (todos los eventos)");
        h.entero(global, 3, resumen.registros());
        h.decimal(global, 4, resumen.global().tprMin());
        h.entero(global, 5, resumen.citasElegibles());
        h.decimal(global, 7, resumen.global().taPct());
        h.entero(global, 8, resumen.consultas());
        h.decimal(global, 10, resumen.global().ncaPct());
    }

    /** Misma estructura que la hoja SPSS_Independientes del Instrumento. */
    private static void spss(HojaExcel h, RecoleccionSesiones.Resumen resumen, String etapa, int grupo) {
        Sheet hoja = h.hojaConEncabezados("SPSS_Independientes", List.of("sesion", "grupo", "etapa", "fecha", "tpr", "ta", "nca"));
        int r = 1;
        for (var s : resumen.sesiones()) {
            Row fila = hoja.createRow(r++);
            h.entero(fila, 0, s.numero());
            h.entero(fila, 1, grupo);
            h.texto(fila, 2, etapa);
            h.texto(fila, 3, s.fecha().format(FECHA_TEXTO));
            h.decimal(fila, 4, s.tprMin());
            h.decimal(fila, 5, s.taPct());
            h.decimal(fila, 6, s.ncaPct());
        }
    }

    private static void metadatos(HojaExcel h, RecoleccionPorSesionUseCase.Exportacion datos) {
        Sheet hoja = h.hojaConEncabezados("Metadatos", List.of("Campo", "Valor"));
        var resumen = datos.resumen();
        var avisos = resumen.avisos();
        String[][] filas = {
                {"Generado", datos.generadoEn().atZone(ZonaHoraria.LIMA).toLocalDateTime().withNano(0).toString()},
                {"Fase", resumen.fase().name()},
                {"Periodo", resumen.desde().format(FECHA_TEXTO) + " al " + resumen.hasta().format(FECHA_TEXTO)},
                {"Días de sesión", resumen.diasSesion().toString()},
                {"TPR", "Alta de paciente por el personal (intranet); inicio y fin marcados por el servidor"},
                {"Ausentismo", "No asistidas sin aviso / citas elegibles (asistió + no asistió) x 100; canceladas excluidas"},
                {"NCA", "Resueltas en el primer contacto sin derivación ni reapertura / consultas con desenlace x 100"},
                {"Citas pasadas sin desenlace", String.valueOf(avisos.citasSinDesenlace())},
                {"Consultas abiertas (no incluidas)", String.valueOf(avisos.consultasAbiertas())},
                {"Registros a revisar", String.valueOf(avisos.registrosSospechosos())},
                {"Eventos fuera de días de sesión (no incluidos)", String.valueOf(avisos.eventosFueraDeSesion())},
                {"Identificación", "Código anónimo PAC-NNNN; sin nombres ni documentos (Ley 29733)"},
        };
        int r = 1;
        for (String[] f : filas) {
            Row fila = hoja.createRow(r++);
            h.texto(fila, 0, f[0]);
            h.texto(fila, 1, f[1]);
        }
        hoja.setColumnWidth(0, 44 * 256);
        hoja.setColumnWidth(1, 90 * 256);
    }

    /** Redondeo a dos decimales para la ficha (el calculo usa los valores exactos). */
    private static final class RecoleccionRedondeo {
        static Double dos(double valor) {
            return Math.round(valor * 100.0) / 100.0;
        }
    }
}

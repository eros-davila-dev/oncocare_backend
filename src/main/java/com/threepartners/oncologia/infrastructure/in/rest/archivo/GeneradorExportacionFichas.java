package com.threepartners.oncologia.infrastructure.in.rest.archivo;

import com.threepartners.oncologia.application.estudio.ExportarDatosEstudioUseCase.ExportacionFichas;
import com.threepartners.oncologia.domain.estudio.AlcanceIndicador;
import com.threepartners.oncologia.domain.estudio.CanalMedicion;
import com.threepartners.oncologia.domain.estudio.IndicadoresEstudio;
import com.threepartners.oncologia.domain.estudio.RegistrosFichas;
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
import java.util.stream.Collectors;

/**
 * Las tres fichas del Anexo 2 de una fase, llenas con los datos del sistema,
 * para adjuntarlas como evidencia de la recoleccion. La hoja "resumen" pone
 * lado a lado el valor que calcula el sistema y el mismo indicador
 * recalculado con formulas de Excel sobre las filas: cualquiera puede
 * verificar que coinciden.
 */
@Component
public class GeneradorExportacionFichas {

    static final List<String> COLUMNAS_TIEMPOS =
            List.of("nro", "codigo", "fecha", "hora_inicio", "hora_fin", "minutos", "segundos", "tipo", "canal");
    static final List<String> COLUMNAS_ASISTENCIAS = List.of("nro", "codigo", "fecha", "hora", "asistio", "origen");
    static final List<String> COLUMNAS_CONSULTAS =
            List.of("nro", "codigo", "fecha", "hora", "medio", "resultado", "resuelta", "resuelta_por_chatbot");

    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public byte[] generar(ExportacionFichas datos) {
        try (var libro = new XSSFWorkbook()) {
            var hoja = new HojaExcel(libro);
            resumen(hoja, datos);
            tiempos(hoja, datos.tiempos());
            asistencias(hoja, datos.asistencias());
            consultas(hoja, datos.consultas());
            libro.setForceFormulaRecalculation(true);
            return hoja.aBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void resumen(HojaExcel hoja, ExportacionFichas datos) {
        Sheet sheet = hoja.hojaConEncabezados("resumen",
                List.of("indicador", "valor_sistema", "recalculado_excel", "base", "formula"));
        IndicadoresEstudio i = datos.indicadores();

        Row tpr = sheet.createRow(1);
        hoja.texto(tpr, 0, "TPR (min) - Instrumento 01");
        hoja.decimal(tpr, 1, i.tiempoPromedioRegistroMinutos());
        hoja.formula(tpr, 2, "IF(COUNT(tiempos!G:G)=0,\"Sin datos\",SUM(tiempos!G:G)/COUNT(tiempos!G:G)/60)");
        hoja.texto(tpr, 3, i.registros() + " registros");
        hoja.texto(tpr, 4, "TPR = suma TRC / NCR");

        Row tns = sheet.createRow(2);
        hoja.texto(tns, 0, "TA (%) - Instrumento 02");
        hoja.decimal(tns, 1, i.tasaAusentismo());
        hoja.formula(tns, 2, "IF(COUNTA(asistencias!E:E)<=1,\"Sin datos\","
                + "COUNTIF(asistencias!E:E,\"NO\")/(COUNTA(asistencias!E:E)-1)*100)");
        hoja.texto(tns, 3, i.inasistencias() + " de " + i.citasConDesenlace() + " citas con desenlace");
        hoja.texto(tns, 4, "TA = NI / (NI + NCC) x 100");

        Row nca = sheet.createRow(3);
        hoja.texto(nca, 0, "NCA (%) - Instrumento 03");
        hoja.decimal(nca, 1, i.nivelConsultasAtendidas());
        hoja.formula(nca, 2, "IF(COUNTA(consultas!G:G)<=1,\"Sin datos\","
                + "COUNTIF(consultas!G:G,\"SI\")/(COUNTA(consultas!G:G)-1)*100)");
        hoja.texto(nca, 3, i.consultasResueltas() + " de " + i.consultasCerradas() + " consultas ("
                + i.consultasResueltasBot() + " por el chatbot)");
        hoja.texto(nca, 4, "NCA = CA / TCR x 100");

        var filtro = datos.filtro();
        String[][] contexto = {
                {"Fase", datos.fase().name()},
                {"Periodo", filtro.periodo().desde() + " a " + filtro.periodo().hasta()},
                {"Alcance", filtro.alcance() == AlcanceIndicador.MUESTRA
                        ? "Muestra: participantes incluidos"
                        : "Todo el sistema (codigo vacio = fuera de la muestra)"},
                {"TPR: tipo y canales", filtro.tipoRegistro().name() + " / " + filtro.canalesRegistro().stream()
                        .map(CanalMedicion::name).sorted().collect(Collectors.joining(", "))},
                {"Generado", datos.generadoEn().atZone(ZonaHoraria.LIMA).format(FECHA_HORA) + " (hora de Lima)"},
                {"Datos personales", "Ninguno: solo el codigo de participante. Sin nombres, documentos ni texto libre."},
        };
        int r = 5;
        for (String[] fila : contexto) {
            Row row = sheet.createRow(r++);
            hoja.negrita(row, 0, fila[0]);
            hoja.texto(row, 1, fila[1]);
        }
        sheet.setColumnWidth(0, 28 * 256);
        sheet.setColumnWidth(3, 40 * 256);
        sheet.setColumnWidth(4, 30 * 256);
    }

    private static void tiempos(HojaExcel hoja, List<RegistrosFichas.Tiempo> filas) {
        Sheet sheet = hoja.hojaConEncabezados("tiempos", COLUMNAS_TIEMPOS);
        int r = 1;
        for (RegistrosFichas.Tiempo t : filas) {
            Row row = sheet.createRow(r);
            hoja.entero(row, 0, r);
            hoja.texto(row, 1, t.codigo());
            hoja.fecha(row, 2, t.fecha());
            hoja.hora(row, 3, t.horaInicio());
            hoja.hora(row, 4, t.horaFin());
            hoja.decimal(row, 5, t.segundos() / 60.0);
            hoja.entero(row, 6, t.segundos());
            hoja.texto(row, 7, t.tipo().name());
            hoja.texto(row, 8, t.canal().name());
            r++;
        }
    }

    private static void asistencias(HojaExcel hoja, List<RegistrosFichas.Asistencia> filas) {
        Sheet sheet = hoja.hojaConEncabezados("asistencias", COLUMNAS_ASISTENCIAS);
        int r = 1;
        for (RegistrosFichas.Asistencia a : filas) {
            Row row = sheet.createRow(r);
            hoja.entero(row, 0, r);
            hoja.texto(row, 1, a.codigo());
            hoja.fecha(row, 2, a.fecha());
            hoja.hora(row, 3, a.hora());
            hoja.texto(row, 4, HojaExcel.siNo(a.asistio()));
            hoja.texto(row, 5, a.origen());
            r++;
        }
    }

    private static void consultas(HojaExcel hoja, List<RegistrosFichas.ConsultaCerrada> filas) {
        Sheet sheet = hoja.hojaConEncabezados("consultas", COLUMNAS_CONSULTAS);
        int r = 1;
        for (RegistrosFichas.ConsultaCerrada k : filas) {
            Row row = sheet.createRow(r);
            hoja.entero(row, 0, r);
            hoja.texto(row, 1, k.codigo());
            hoja.fecha(row, 2, k.fecha());
            hoja.hora(row, 3, k.hora());
            hoja.texto(row, 4, k.canal().name());
            hoja.texto(row, 5, k.resultado().name());
            hoja.texto(row, 6, HojaExcel.siNo(k.resultado() != ResultadoConsulta.NO_RESUELTA));
            hoja.texto(row, 7, HojaExcel.siNo(k.resultado() == ResultadoConsulta.RESUELTA_BOT));
            r++;
        }
    }
}

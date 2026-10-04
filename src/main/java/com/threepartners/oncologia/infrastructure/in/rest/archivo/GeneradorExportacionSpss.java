package com.threepartners.oncologia.infrastructure.in.rest.archivo;

import com.threepartners.oncologia.application.estudio.ExportarDatosEstudioUseCase.ExportacionSpss;
import com.threepartners.oncologia.domain.estudio.CanalMedicion;
import com.threepartners.oncologia.domain.estudio.ComparativoIndicadores;
import com.threepartners.oncologia.domain.estudio.ComparativoIndicadores.Variacion;
import com.threepartners.oncologia.domain.estudio.FilaPareada;
import com.threepartners.oncologia.domain.estudio.IndicadoresEstudio;
import com.threepartners.oncologia.domain.estudio.PeriodoMedicion;
import com.threepartners.oncologia.domain.estudio.ResultadoWilcoxon;
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
 * Libro para SPSS 29. La hoja "pareado" va primera porque es la que se abre
 * en SPSS (Archivo &gt; Abrir &gt; Datos, "leer nombres de variables de la
 * primera fila"): una fila por participante y una columna por indicador y
 * fase, el formato que pide la prueba de Wilcoxon para muestras relacionadas.
 * Los nombres de variable cumplen las reglas de SPSS (empiezan con letra, sin
 * espacios ni tildes).
 */
@Component
public class GeneradorExportacionSpss {

    static final List<String> VARIABLES_PAREADO =
            List.of("codigo", "tpr_pre", "tpr_post", "tns_pre", "tns_post", "nca_pre", "nca_post");

    static final List<String> VARIABLES_DETALLE = List.of("codigo", "fase", "registros", "tpr_min",
            "inasistencias", "citas_cumplidas", "citas_desenlace", "tns_pct", "consultas_cerradas",
            "consultas_resueltas", "consultas_resueltas_bot", "nca_pct", "nca_auto_pct");

    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public byte[] generar(ExportacionSpss datos) {
        try (var libro = new XSSFWorkbook()) {
            var hoja = new HojaExcel(libro);
            pareado(hoja, datos.filas());
            detalle(hoja, datos.filas());
            resumen(hoja, datos);
            diccionario(hoja);
            metadatos(hoja, datos);
            return hoja.aBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void pareado(HojaExcel hoja, List<FilaPareada> filas) {
        Sheet sheet = hoja.hojaConEncabezados("pareado", VARIABLES_PAREADO);
        int r = 1;
        for (FilaPareada f : filas) {
            Row row = sheet.createRow(r++);
            hoja.texto(row, 0, f.codigo());
            hoja.decimal(row, 1, f.pretest().tiempoPromedioRegistroMinutos());
            hoja.decimal(row, 2, f.postest().tiempoPromedioRegistroMinutos());
            hoja.decimal(row, 3, f.pretest().tasaAusentismo());
            hoja.decimal(row, 4, f.postest().tasaAusentismo());
            hoja.decimal(row, 5, f.pretest().nivelConsultasAtendidas());
            hoja.decimal(row, 6, f.postest().nivelConsultasAtendidas());
        }
    }

    /** Formato largo (participante x fase) con el n detras de cada porcentaje. */
    private static void detalle(HojaExcel hoja, List<FilaPareada> filas) {
        Sheet sheet = hoja.hojaConEncabezados("detalle", VARIABLES_DETALLE);
        int r = 1;
        for (FilaPareada f : filas) {
            r = filaDetalle(hoja, sheet, r, f.codigo(), "PRETEST", f.pretest());
            r = filaDetalle(hoja, sheet, r, f.codigo(), "POSTEST", f.postest());
        }
    }

    private static int filaDetalle(HojaExcel hoja, Sheet sheet, int r, String codigo, String fase, IndicadoresEstudio i) {
        Row row = sheet.createRow(r);
        hoja.texto(row, 0, codigo);
        hoja.texto(row, 1, fase);
        hoja.entero(row, 2, i.registros());
        hoja.decimal(row, 3, i.tiempoPromedioRegistroMinutos());
        hoja.entero(row, 4, i.inasistencias());
        hoja.entero(row, 5, i.citasCumplidas());
        hoja.entero(row, 6, i.citasConDesenlace());
        hoja.decimal(row, 7, i.tasaAusentismo());
        hoja.entero(row, 8, i.consultasCerradas());
        hoja.entero(row, 9, i.consultasResueltas());
        hoja.entero(row, 10, i.consultasResueltasBot());
        hoja.decimal(row, 11, i.nivelConsultasAtendidas());
        hoja.decimal(row, 12, i.nivelConsultasAtendidasAutomatico());
        return r + 1;
    }

    private static void resumen(HojaExcel hoja, ExportacionSpss datos) {
        Sheet sheet = hoja.hojaConEncabezados("resumen",
                List.of("indicador", "pretest", "postest", "diferencia", "variacion_pct", "n_pretest", "n_postest"));
        ComparativoIndicadores c = datos.comparativo();
        filaResumen(hoja, sheet, 1, "TPR (min)", c.pretest().tiempoPromedioRegistroMinutos(),
                c.postest().tiempoPromedioRegistroMinutos(), c.tiempoPromedioRegistro(),
                c.pretest().registros() + " registros", c.postest().registros() + " registros");
        filaResumen(hoja, sheet, 2, "TA (%)", c.pretest().tasaAusentismo(), c.postest().tasaAusentismo(),
                c.tasaAusentismo(), c.pretest().citasConDesenlace() + " citas con desenlace",
                c.postest().citasConDesenlace() + " citas con desenlace");
        filaResumen(hoja, sheet, 3, "NCA (%)", c.pretest().nivelConsultasAtendidas(),
                c.postest().nivelConsultasAtendidas(), c.nivelConsultasAtendidas(),
                c.pretest().consultasCerradas() + " consultas", c.postest().consultasCerradas() + " consultas");

        int r = 5;
        hoja.titulo(sheet.createRow(r++), 0, "Prueba de Wilcoxon para muestras relacionadas (vista preliminar)");
        Row encabezado = sheet.createRow(r++);
        List<String> columnas = List.of("indicador", "pares", "empates", "n", "rangos_negativos", "rangos_positivos",
                "suma_rangos_neg", "suma_rangos_pos", "mediana_pre", "mediana_post", "z", "p_asintotica",
                "p_exacta", "r_efecto");
        for (int col = 0; col < columnas.size(); col++) {
            hoja.negrita(encabezado, col, columnas.get(col));
        }
        filaWilcoxon(hoja, sheet.createRow(r++), "TPR", datos.analisis().tiempoPromedioRegistro());
        filaWilcoxon(hoja, sheet.createRow(r++), "TA", datos.analisis().tasaAusentismo());
        filaWilcoxon(hoja, sheet.createRow(r++), "NCA", datos.analisis().nivelConsultasAtendidas());

        r++;
        for (String nota : List.of(
                "Diferencia = postest - pretest. Rangos negativos: postest < pretest (mejora esperada en TPR y TA); "
                        + "positivos: postest > pretest (mejora esperada en NCA).",
                "Cada indicador usa solo los participantes con valor en ambas fases; los empates (diferencia 0) se descartan.",
                "Z con el menor total de rangos, correccion por empates y sin correccion por continuidad (como SPSS). "
                        + "p bilateral; alfa = 0,05.",
                "Calculado por el sistema como anticipo. El analisis que se reporta en la tesis (Shapiro-Wilk y Wilcoxon) "
                        + "se hace en SPSS 29 con la hoja 'pareado'.")) {
            hoja.texto(sheet.createRow(r++), 0, nota);
        }
        sheet.setColumnWidth(0, 16 * 256);
    }

    private static void filaResumen(HojaExcel hoja, Sheet sheet, int r, String nombre, Double pre, Double post,
                                    Variacion variacion, String nPre, String nPost) {
        Row row = sheet.createRow(r);
        hoja.texto(row, 0, nombre);
        hoja.decimal(row, 1, pre);
        hoja.decimal(row, 2, post);
        hoja.decimal(row, 3, variacion.diferencia());
        hoja.decimal(row, 4, variacion.porcentaje());
        hoja.texto(row, 5, nPre);
        hoja.texto(row, 6, nPost);
    }

    private static void filaWilcoxon(HojaExcel hoja, Row row, String nombre, ResultadoWilcoxon w) {
        hoja.texto(row, 0, nombre);
        hoja.entero(row, 1, w.pares());
        hoja.entero(row, 2, w.empates());
        hoja.entero(row, 3, w.n());
        hoja.entero(row, 4, w.rangosNegativos());
        hoja.entero(row, 5, w.rangosPositivos());
        hoja.decimal(row, 6, w.sumaRangosNegativos());
        hoja.decimal(row, 7, w.sumaRangosPositivos());
        hoja.decimal(row, 8, w.medianaPretest());
        hoja.decimal(row, 9, w.medianaPostest());
        hoja.decimal(row, 10, w.z());
        hoja.decimal(row, 11, w.pAsintotica());
        hoja.decimal(row, 12, w.pExacta());
        hoja.decimal(row, 13, w.tamanoEfecto());
    }

    /** Para pegar en la Vista de variables de SPSS y en el anexo de la tesis. */
    private static void diccionario(HojaExcel hoja) {
        Sheet sheet = hoja.hojaConEncabezados("diccionario",
                List.of("variable", "hoja", "etiqueta", "tipo", "medida", "definicion"));
        String[][] filas = {
                {"codigo", "pareado, detalle", "Codigo del participante", "Cadena", "Nominal",
                        "Pnn asignado al dar el consentimiento. No hay nombres ni documentos en este archivo."},
                {"tpr_pre / tpr_post", "pareado", "Tiempo promedio de registro (min)", "Numerico", "Escala",
                        "TPR = suma de TRC / NCR. TRC medido por el servidor (inicio al abrir el formulario, fin al guardar)."},
                {"tns_pre / tns_post", "pareado", "Tasa de ausentismo (%)", "Numerico", "Escala",
                        "TA = NI / (NI + NCC) x 100. Solo citas con desenlace (atendida o no asistio)."},
                {"nca_pre / nca_post", "pareado", "Nivel de consultas atendidas (%)", "Numerico", "Escala",
                        "NCA = CA / TCR x 100. Consultas con resultado final; resueltas por el chatbot o por el personal."},
                {"fase", "detalle", "Fase del estudio", "Cadena", "Nominal", "PRETEST o POSTEST, segun las fechas configuradas."},
                {"registros", "detalle", "NCR: numero de registros", "Numerico", "Escala", "Base del TPR."},
                {"tpr_min", "detalle", "TPR (min)", "Numerico", "Escala", "Igual que tpr_pre/tpr_post."},
                {"inasistencias", "detalle", "NI: inasistencias", "Numerico", "Escala", "Citas NO_ASISTIO."},
                {"citas_cumplidas", "detalle", "NCC: citas cumplidas", "Numerico", "Escala", "Citas ATENDIDA."},
                {"citas_desenlace", "detalle", "NI + NCC", "Numerico", "Escala", "Base de la TA."},
                {"tns_pct", "detalle", "TA (%)", "Numerico", "Escala", "Igual que tns_pre/tns_post."},
                {"consultas_cerradas", "detalle", "TCR: consultas registradas", "Numerico", "Escala",
                        "Consultas con resultado final. Las escaladas sin respuesta y las anuladas no cuentan."},
                {"consultas_resueltas", "detalle", "CA: consultas atendidas", "Numerico", "Escala",
                        "Resueltas por el chatbot o por el personal."},
                {"consultas_resueltas_bot", "detalle", "Consultas resueltas por el chatbot", "Numerico", "Escala",
                        "Aporte especifico del chatbot (subconjunto de CA)."},
                {"nca_pct", "detalle", "NCA (%)", "Numerico", "Escala", "Igual que nca_pre/nca_post."},
                {"nca_auto_pct", "detalle", "NCA automatico (%)", "Numerico", "Escala",
                        "Resueltas solo por el chatbot / TCR x 100. Analisis complementario."},
        };
        for (int i = 0; i < filas.length; i++) {
            Row row = sheet.createRow(i + 1);
            for (int c = 0; c < filas[i].length; c++) {
                hoja.texto(row, c, filas[i][c]);
            }
        }
        hoja.texto(sheet.createRow(filas.length + 2), 0,
                "Valores perdidos: una celda vacia significa 'sin datos' (denominador cero), no 0. "
                        + "SPSS la lee como perdido del sistema y la excluye de la prueba.");
        sheet.setColumnWidth(5, 95 * 256);
    }

    private static void metadatos(HojaExcel hoja, ExportacionSpss datos) {
        Sheet sheet = hoja.hojaConEncabezados("metadatos", List.of("dato", "valor"));
        ComparativoIndicadores c = datos.comparativo();
        String[][] filas = {
                {"Estudio", "Sistema web con chatbot para la gestion de pacientes en una fundacion oncologica privada de Lima, 2026"},
                {"Generado", datos.generadoEn().atZone(ZonaHoraria.LIMA).format(FECHA_HORA) + " (hora de Lima)"},
                {"Pretest", periodo(c.periodoPretest())},
                {"Postest", periodo(c.periodoPostest())},
                {"Alcance", "Muestra: participantes incluidos con consentimiento informado"},
                {"Participantes", String.valueOf(datos.filas().size())},
                {"TPR: tipo de registro", datos.tipoRegistro().name()},
                {"TPR: canales", datos.canalesRegistro().stream().map(CanalMedicion::name).sorted()
                        .collect(Collectors.joining(", "))},
                {"Anonimizacion", "Solo codigos de participante. La tabla de correspondencia codigo-paciente queda en el sistema."},
        };
        for (int i = 0; i < filas.length; i++) {
            Row row = sheet.createRow(i + 1);
            hoja.texto(row, 0, filas[i][0]);
            hoja.texto(row, 1, filas[i][1]);
        }
        sheet.setColumnWidth(0, 24 * 256);
        sheet.setColumnWidth(1, 100 * 256);
    }

    private static String periodo(PeriodoMedicion p) {
        return p.desde() + " a " + p.hasta();
    }
}

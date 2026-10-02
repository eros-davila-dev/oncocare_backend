package com.threepartners.oncologia.infrastructure.in.rest.archivo;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Escritura de hojas de exportacion con reglas pensadas para SPSS y Excel:
 * un null queda como celda vacia (SPSS lo lee como valor perdido del sistema,
 * nunca como 0), los numeros se guardan como numero y las fechas y horas
 * como fecha/hora de Excel, no como texto.
 */
final class HojaExcel {

    private final XSSFWorkbook libro;
    private final CellStyle encabezado;
    private final CellStyle decimal;
    private final CellStyle entero;
    private final CellStyle fecha;
    private final CellStyle hora;
    private final CellStyle titulo;

    HojaExcel(XSSFWorkbook libro) {
        this.libro = libro;
        var formatos = libro.createDataFormat();
        Font negrita = libro.createFont();
        negrita.setBold(true);
        encabezado = libro.createCellStyle();
        encabezado.setFont(negrita);
        Font grande = libro.createFont();
        grande.setBold(true);
        grande.setFontHeightInPoints((short) 13);
        titulo = libro.createCellStyle();
        titulo.setFont(grande);
        decimal = libro.createCellStyle();
        decimal.setDataFormat(formatos.getFormat("0.00"));
        entero = libro.createCellStyle();
        entero.setDataFormat(formatos.getFormat("0"));
        fecha = libro.createCellStyle();
        fecha.setDataFormat(formatos.getFormat("yyyy-mm-dd"));
        hora = libro.createCellStyle();
        hora.setDataFormat(formatos.getFormat("hh:mm"));
    }

    /** Hoja con su fila de encabezados congelada. */
    Sheet hojaConEncabezados(String nombre, List<String> columnas) {
        Sheet hoja = libro.createSheet(nombre);
        Row fila = hoja.createRow(0);
        for (int c = 0; c < columnas.size(); c++) {
            texto(fila, c, columnas.get(c)).setCellStyle(encabezado);
            hoja.setColumnWidth(c, Math.max(12, columnas.get(c).length() + 3) * 256);
        }
        hoja.createFreezePane(0, 1);
        return hoja;
    }

    Cell texto(Row fila, int columna, String valor) {
        Cell celda = fila.createCell(columna);
        if (valor != null) {
            celda.setCellValue(valor);
        }
        return celda;
    }

    void titulo(Row fila, int columna, String valor) {
        texto(fila, columna, valor).setCellStyle(titulo);
    }

    void negrita(Row fila, int columna, String valor) {
        texto(fila, columna, valor).setCellStyle(encabezado);
    }

    void decimal(Row fila, int columna, Double valor) {
        Cell celda = fila.createCell(columna);
        if (valor != null) {
            celda.setCellValue(valor);
            celda.setCellStyle(decimal);
        }
    }

    void entero(Row fila, int columna, long valor) {
        Cell celda = fila.createCell(columna);
        celda.setCellValue(valor);
        celda.setCellStyle(entero);
    }

    void fecha(Row fila, int columna, LocalDate valor) {
        Cell celda = fila.createCell(columna);
        if (valor != null) {
            celda.setCellValue(valor);
            celda.setCellStyle(fecha);
        }
    }

    void hora(Row fila, int columna, LocalTime valor) {
        Cell celda = fila.createCell(columna);
        if (valor != null) {
            celda.setCellValue(valor.toSecondOfDay() / 86_400.0);
            celda.setCellStyle(hora);
        }
    }

    void formula(Row fila, int columna, String formula) {
        Cell celda = fila.createCell(columna);
        celda.setCellFormula(formula);
        celda.setCellStyle(decimal);
    }

    static String siNo(boolean valor) {
        return valor ? "SI" : "NO";
    }

    byte[] aBytes() {
        try (var salida = new ByteArrayOutputStream()) {
            libro.write(salida);
            return salida.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}

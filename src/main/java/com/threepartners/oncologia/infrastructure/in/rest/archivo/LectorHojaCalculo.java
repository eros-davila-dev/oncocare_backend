package com.threepartners.oncologia.infrastructure.in.rest.archivo;

import com.threepartners.oncologia.domain.shared.exception.ValidacionDeNegocioException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Convierte un .xlsx o .csv en filas "encabezado normalizado -> texto". Las
 * celdas de fecha/hora de Excel se traducen a ISO (AAAA-MM-DD / HH:MM) en vez
 * de usar el formato visual, que depende del idioma del Excel de quien lo
 * lleno.
 */
@Component
public class LectorHojaCalculo {

    private static final long TAMANO_MAXIMO_BYTES = 5L * 1024 * 1024;
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final int ANIO_BASE_EXCEL_SOLO_HORA = 1900;

    public List<Map<String, String>> leer(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ValidacionDeNegocioException("Adjunte un archivo .xlsx o .csv");
        }
        if (archivo.getSize() > TAMANO_MAXIMO_BYTES) {
            throw new ValidacionDeNegocioException("El archivo supera el tamano maximo de 5 MB");
        }
        String nombre = archivo.getOriginalFilename() != null ? archivo.getOriginalFilename().toLowerCase(Locale.ROOT) : "";
        try (InputStream entrada = archivo.getInputStream()) {
            if (nombre.endsWith(".csv")) {
                return leerCsv(entrada);
            }
            if (nombre.endsWith(".xlsx") || nombre.endsWith(".xls")) {
                return leerExcel(entrada);
            }
        } catch (IOException e) {
            throw new ValidacionDeNegocioException("No se pudo leer el archivo: " + e.getMessage());
        }
        throw new ValidacionDeNegocioException("Formato no soportado: use .xlsx o .csv");
    }

    public static String normalizarEncabezado(String encabezado) {
        String sinTildes = Normalizer.normalize(encabezado == null ? "" : encabezado, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sinTildes.toLowerCase(Locale.ROOT)
                .replaceAll("\\(.*?\\)", "")
                .replaceAll("[?*]", "")
                .strip()
                .replaceAll("[\\s\\-/.]+", "_");
    }

    private List<Map<String, String>> leerExcel(InputStream entrada) throws IOException {
        try (Workbook libro = WorkbookFactory.create(entrada)) {
            Sheet hoja = libro.getSheetAt(0);
            DataFormatter formateador = new DataFormatter(Locale.ROOT);
            Row filaEncabezados = hoja.getRow(hoja.getFirstRowNum());
            if (filaEncabezados == null) {
                return List.of();
            }
            List<String> encabezados = new ArrayList<>();
            for (Cell celda : filaEncabezados) {
                encabezados.add(normalizarEncabezado(formateador.formatCellValue(celda)));
            }
            List<Map<String, String>> filas = new ArrayList<>();
            for (int i = hoja.getFirstRowNum() + 1; i <= hoja.getLastRowNum(); i++) {
                Row fila = hoja.getRow(i);
                Map<String, String> valores = new LinkedHashMap<>();
                for (int c = 0; c < encabezados.size(); c++) {
                    Cell celda = fila != null ? fila.getCell(c) : null;
                    valores.put(encabezados.get(c), celda != null ? texto(celda, formateador) : "");
                }
                filas.add(valores);
            }
            return filas;
        }
    }

    private static String texto(Cell celda, DataFormatter formateador) {
        CellType tipo = celda.getCellType() == CellType.FORMULA ? celda.getCachedFormulaResultType() : celda.getCellType();
        if (tipo == CellType.NUMERIC) {
            if (DateUtil.isCellDateFormatted(celda)) {
                LocalDateTime valor = celda.getLocalDateTimeCellValue();
                if (valor.getYear() <= ANIO_BASE_EXCEL_SOLO_HORA) {
                    return valor.toLocalTime().format(HORA);
                }
                return valor.toLocalTime().equals(java.time.LocalTime.MIDNIGHT)
                        ? valor.toLocalDate().toString()
                        : valor.toLocalDate() + " " + valor.toLocalTime().format(HORA);
            }
            // Documentos de identidad escritos como numero: sin notacion cientifica ni ".0"
            return BigDecimal.valueOf(celda.getNumericCellValue()).stripTrailingZeros().toPlainString();
        }
        return formateador.formatCellValue(celda).strip();
    }

    private List<Map<String, String>> leerCsv(InputStream entrada) throws IOException {
        var lector = new BufferedReader(new InputStreamReader(entrada, StandardCharsets.UTF_8));
        String primeraLinea = lector.readLine();
        if (primeraLinea == null) {
            return List.of();
        }
        primeraLinea = primeraLinea.replace("﻿", "");
        char separador = contar(primeraLinea, ';') > contar(primeraLinea, ',') ? ';' : ',';
        List<String> encabezados = dividir(primeraLinea, separador).stream()
                .map(LectorHojaCalculo::normalizarEncabezado)
                .toList();
        List<Map<String, String>> filas = new ArrayList<>();
        String linea;
        while ((linea = lector.readLine()) != null) {
            List<String> celdas = dividir(linea, separador);
            Map<String, String> valores = new LinkedHashMap<>();
            for (int c = 0; c < encabezados.size(); c++) {
                valores.put(encabezados.get(c), c < celdas.size() ? celdas.get(c).strip() : "");
            }
            filas.add(valores);
        }
        return filas;
    }

    private static long contar(String texto, char caracter) {
        return texto.chars().filter(c -> c == caracter).count();
    }

    /** CSV minimo con soporte de comillas dobles (campos con separador o comillas escapadas ""). */
    static List<String> dividir(String linea, char separador) {
        List<String> campos = new ArrayList<>();
        StringBuilder actual = new StringBuilder();
        boolean entreComillas = false;
        for (int i = 0; i < linea.length(); i++) {
            char c = linea.charAt(i);
            if (c == '"') {
                if (entreComillas && i + 1 < linea.length() && linea.charAt(i + 1) == '"') {
                    actual.append('"');
                    i++;
                } else {
                    entreComillas = !entreComillas;
                }
            } else if (c == separador && !entreComillas) {
                campos.add(actual.toString());
                actual.setLength(0);
            } else {
                actual.append(c);
            }
        }
        campos.add(actual.toString());
        return campos;
    }
}

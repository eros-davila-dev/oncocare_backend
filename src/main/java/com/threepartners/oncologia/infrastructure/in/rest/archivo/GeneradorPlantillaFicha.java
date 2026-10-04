package com.threepartners.oncologia.infrastructure.in.rest.archivo;

import com.threepartners.oncologia.application.estudio.FichasEstudio.TipoFicha;
import com.threepartners.oncologia.application.estudio.ImportarFichaEstudioUseCase;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * Plantilla .xlsx de cada ficha del Anexo 2, con los encabezados que espera
 * la importacion, una fila de ejemplo y una hoja de instrucciones. Todas las
 * columnas tienen formato de texto: asi Excel no convierte un DNI con cero
 * inicial en numero ni una hora en fraccion de dia.
 */
@Component
public class GeneradorPlantillaFicha {

    public byte[] generar(TipoFicha ficha) {
        try (var libro = new XSSFWorkbook(); var salida = new ByteArrayOutputStream()) {
            CellStyle texto = libro.createCellStyle();
            texto.setDataFormat(libro.createDataFormat().getFormat("@"));
            CellStyle encabezado = libro.createCellStyle();
            Font negrita = libro.createFont();
            negrita.setBold(true);
            encabezado.setFont(negrita);
            encabezado.setDataFormat(libro.createDataFormat().getFormat("@"));

            Sheet hoja = libro.createSheet(ficha.name().toLowerCase());
            List<String> columnas = ImportarFichaEstudioUseCase.columnas(ficha);
            Row filaEncabezado = hoja.createRow(0);
            Row filaEjemplo = hoja.createRow(1);
            List<String> ejemplo = ejemplo(ficha);
            for (int c = 0; c < columnas.size(); c++) {
                var celda = filaEncabezado.createCell(c);
                celda.setCellValue(columnas.get(c).replace("?", ""));
                celda.setCellStyle(encabezado);
                var celdaEjemplo = filaEjemplo.createCell(c);
                celdaEjemplo.setCellValue(ejemplo.get(c));
                celdaEjemplo.setCellStyle(texto);
                hoja.setDefaultColumnStyle(c, texto);
                hoja.setColumnWidth(c, 18 * 256);
            }

            Sheet ayuda = libro.createSheet("instrucciones");
            List<String> lineas = instrucciones(ficha, columnas);
            for (int i = 0; i < lineas.size(); i++) {
                ayuda.createRow(i).createCell(0).setCellValue(lineas.get(i));
            }
            ayuda.setColumnWidth(0, 110 * 256);

            libro.write(salida);
            return salida.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static List<String> ejemplo(TipoFicha ficha) {
        return switch (ficha) {
            case TIEMPOS -> List.of("P01", "2026-10-05", "09:10", "09:24", "CITA", "");
            case ASISTENCIAS -> List.of("P01", "2026-10-05", "10:30", "NO", "Control oncologico");
            case CONSULTAS -> List.of("2026-10-05", "11:00", "WHATSAPP", "P01", "Informacion de cita", "SI", "");
        };
    }

    private static List<String> instrucciones(TipoFicha ficha, List<String> columnas) {
        String titulo = switch (ficha) {
            case TIEMPOS -> "Instrumento 01 - Ficha de registro de tiempos (indicador TPR). Solo fechas del PRETEST.";
            case ASISTENCIAS -> "Instrumento 02 - Ficha de registro de ausentismo (indicador TA). Solo fechas del PRETEST.";
            case CONSULTAS -> "Instrumento 03 - Ficha de registro del sistema (indicador NCA). Cualquier fase abierta.";
        };
        return List.of(
                titulo,
                "",
                "Columnas (las marcadas con ? son opcionales): " + String.join(", ", columnas),
                "paciente: codigo de participante (P01) o documento de identidad del paciente.",
                "fecha: AAAA-MM-DD o DD/MM/AAAA. No puede ser futura y debe caer dentro de una fase configurada y abierta.",
                "horas: HH:MM en formato 24 horas (ej. 14:05).",
                "asistio / resuelta: SI o NO.",
                "medio (consultas): WHATSAPP, LLAMADA o PRESENCIAL.",
                "tipo (tiempos): CITA (por defecto), PACIENTE o ACTUALIZACION.",
                "",
                "Borre la fila de ejemplo antes de importar. La importacion es todo o nada: si una fila tiene",
                "errores no se guarda ninguna; use 'Validar' para revisar antes de 'Importar'.");
    }
}

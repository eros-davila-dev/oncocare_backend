package com.threepartners.oncologia.infrastructure.in.rest.archivo;

import com.threepartners.oncologia.application.estudio.ExportarDatosEstudioUseCase.ExportacionFichas;
import com.threepartners.oncologia.application.estudio.ExportarDatosEstudioUseCase.ExportacionSpss;
import com.threepartners.oncologia.domain.estudio.AlcanceIndicador;
import com.threepartners.oncologia.domain.estudio.AnalisisPareado;
import com.threepartners.oncologia.domain.estudio.CanalConsulta;
import com.threepartners.oncologia.domain.estudio.CanalMedicion;
import com.threepartners.oncologia.domain.estudio.ComparativoIndicadores;
import com.threepartners.oncologia.domain.estudio.ConteosIndicadores;
import com.threepartners.oncologia.domain.estudio.Fase;
import com.threepartners.oncologia.domain.estudio.FilaPareada;
import com.threepartners.oncologia.domain.estudio.FiltroIndicadores;
import com.threepartners.oncologia.domain.estudio.IndicadoresEstudio;
import com.threepartners.oncologia.domain.estudio.PeriodoMedicion;
import com.threepartners.oncologia.domain.estudio.RegistrosFichas;
import com.threepartners.oncologia.domain.estudio.ResultadoConsulta;
import com.threepartners.oncologia.domain.estudio.TipoMedicion;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class GeneradoresExportacionTest {

    private static final PeriodoMedicion PRE = new PeriodoMedicion(LocalDate.of(2026, 8, 15), LocalDate.of(2026, 9, 30));
    private static final PeriodoMedicion POST = new PeriodoMedicion(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 12, 31));
    private static final Instant AHORA = Instant.parse("2026-10-01T15:00:00Z");

    @Test
    void elLibroSpssTieneLaTablaPareadaPrimeroYLosSinDatosComoCeldaVacia() throws IOException {
        var p01Pre = IndicadoresEstudio.de(new ConteosIndicadores(2, 1200, 1, 1, 1, 0, 2));
        var p01Post = IndicadoresEstudio.de(new ConteosIndicadores(1, 300, 0, 2, 2, 1, 2));
        var vacio = IndicadoresEstudio.de(ConteosIndicadores.vacios());
        List<FilaPareada> filas = List.of(new FilaPareada("P01", p01Pre, p01Post), new FilaPareada("P02", p01Pre, vacio));
        var datos = new ExportacionSpss(AHORA, TipoMedicion.REGISTRO_CITA, CanalMedicion.porDefectoParaHipotesis(), filas,
                ComparativoIndicadores.de(AlcanceIndicador.MUESTRA, PRE, POST, p01Pre, p01Post), AnalisisPareado.de(filas));

        try (Workbook libro = leer(new GeneradorExportacionSpss().generar(datos))) {
            assertThat(nombresDeHojas(libro)).containsExactly("pareado", "detalle", "resumen", "diccionario", "metadatos");

            Sheet pareado = libro.getSheet("pareado");
            assertThat(textos(pareado.getRow(0))).containsExactlyElementsOf(GeneradorExportacionSpss.VARIABLES_PAREADO);
            Row p01 = pareado.getRow(1);
            assertThat(p01.getCell(0).getStringCellValue()).isEqualTo("P01");
            assertThat(p01.getCell(1).getNumericCellValue()).isEqualTo(10.0);   // 1200 s / 2 registros
            assertThat(p01.getCell(2).getNumericCellValue()).isEqualTo(5.0);
            assertThat(p01.getCell(3).getNumericCellValue()).isEqualTo(50.0);
            // P02 sin postest: celda vacia (perdido del sistema en SPSS), nunca 0
            assertThat(pareado.getRow(2).getCell(2).getCellType()).isEqualTo(CellType.BLANK);

            assertThat(libro.getSheet("detalle").getLastRowNum()).isEqualTo(4);   // 2 participantes x 2 fases
            assertThat(new DataFormatter().formatCellValue(libro.getSheet("resumen").getRow(7).getCell(0))).isEqualTo("TPR");
        }
    }

    @Test
    void lasFichasSeRecalculanEnExcelYDanLoMismoQueElSistema() throws IOException {
        var tiempos = List.of(
                new RegistrosFichas.Tiempo("P01", LocalDate.of(2026, 9, 10), LocalTime.of(9, 0), LocalTime.of(9, 10),
                        600, TipoMedicion.REGISTRO_CITA, CanalMedicion.MANUAL),
                new RegistrosFichas.Tiempo("P02", LocalDate.of(2026, 9, 11), LocalTime.of(9, 0), LocalTime.of(9, 5),
                        300, TipoMedicion.REGISTRO_CITA, CanalMedicion.MANUAL));
        var asistencias = new ArrayList<RegistrosFichas.Asistencia>();
        IntStream.range(0, 3).forEach(i -> asistencias.add(new RegistrosFichas.Asistencia("P01",
                LocalDate.of(2026, 9, 1 + i), LocalTime.of(10, 0), i == 0, "CAPTURA_PRETEST")));
        var consultas = List.of(
                new RegistrosFichas.ConsultaCerrada("P01", LocalDate.of(2026, 9, 5), LocalTime.of(11, 0),
                        CanalConsulta.LLAMADA, ResultadoConsulta.RESUELTA_PERSONAL),
                new RegistrosFichas.ConsultaCerrada(null, LocalDate.of(2026, 9, 6), LocalTime.of(11, 0),
                        CanalConsulta.CHATBOT_WEB, ResultadoConsulta.NO_RESUELTA));
        // Indicadores del sistema para esas mismas filas
        var indicadores = IndicadoresEstudio.de(new ConteosIndicadores(2, 900, 2, 1, 1, 0, 2));
        var datos = new ExportacionFichas(AHORA, Fase.PRETEST, FiltroIndicadores.deHipotesis(PRE, AlcanceIndicador.GLOBAL),
                indicadores, tiempos, asistencias, consultas);

        try (Workbook libro = leer(new GeneradorExportacionFichas().generar(datos))) {
            assertThat(nombresDeHojas(libro)).containsExactly("resumen", "tiempos", "asistencias", "consultas");
            var evaluador = libro.getCreationHelper().createFormulaEvaluator();
            Sheet resumen = libro.getSheet("resumen");
            for (int r = 1; r <= 3; r++) {
                Row fila = resumen.getRow(r);
                double sistema = fila.getCell(1).getNumericCellValue();
                double excel = evaluador.evaluate(fila.getCell(2)).getNumberValue();
                assertThat(excel).as(fila.getCell(0).getStringCellValue()).isCloseTo(sistema, within(1e-9));
            }
            assertThat(libro.getSheet("asistencias").getRow(1).getCell(4).getStringCellValue()).isEqualTo("SI");
            // Dato fuera de la muestra: sin codigo
            assertThat(libro.getSheet("consultas").getRow(2).getCell(1).getCellType()).isEqualTo(CellType.BLANK);
        }
    }

    @Test
    void ningunaHojaContieneDatosPersonales() throws IOException {
        var vacio = IndicadoresEstudio.de(ConteosIndicadores.vacios());
        List<FilaPareada> filas = List.of(new FilaPareada("P07", vacio, vacio));
        var spss = new ExportacionSpss(AHORA, TipoMedicion.REGISTRO_CITA, CanalMedicion.porDefectoParaHipotesis(), filas,
                ComparativoIndicadores.de(AlcanceIndicador.MUESTRA, PRE, POST, vacio, vacio), AnalisisPareado.de(filas));

        try (Workbook libro = leer(new GeneradorExportacionSpss().generar(spss))) {
            var formato = new DataFormatter();
            List<String> encabezados = new ArrayList<>();
            libro.forEach(hoja -> encabezados.addAll(textos(hoja.getRow(0))));
            assertThat(encabezados).noneMatch(e -> e.matches("(?i).*(nombre|documento|dni|telefono|correo|email).*"));
            assertThat(formato.formatCellValue(libro.getSheet("pareado").getRow(1).getCell(0))).isEqualTo("P07");
        }
    }

    private static Workbook leer(byte[] contenido) throws IOException {
        return new XSSFWorkbook(new ByteArrayInputStream(contenido));
    }

    private static List<String> nombresDeHojas(Workbook libro) {
        return IntStream.range(0, libro.getNumberOfSheets()).mapToObj(libro::getSheetName).toList();
    }

    private static List<String> textos(Row fila) {
        List<String> valores = new ArrayList<>();
        for (Cell celda : fila) {
            valores.add(celda.getStringCellValue());
        }
        return valores;
    }
}

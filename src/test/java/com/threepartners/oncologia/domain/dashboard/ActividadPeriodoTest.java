package com.threepartners.oncologia.domain.dashboard;

import com.threepartners.oncologia.domain.estudio.CanalConsulta;
import com.threepartners.oncologia.domain.estudio.CanalMedicion;
import com.threepartners.oncologia.domain.estudio.CategoriaConsulta;
import com.threepartners.oncologia.domain.estudio.ResultadoConsulta;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ActividadPeriodoTest {

    private static final LocalDate LUNES = LocalDate.of(2026, 10, 5);
    private static final LocalDate MARTES = LUNES.plusDays(1);
    private static final LocalTime NUEVE = LocalTime.of(9, 0);

    @Test
    void resumeLosCuatroProcesosConLasReglasDeLaRecoleccion() {
        var filas = new ActividadPeriodo.Filas(
                List.of(registro(LUNES, 4.0), registro(LUNES, 6.0), registro(MARTES, 2.0)),
                List.of(cita(LUNES, "ATENDIDA", true), cita(LUNES, "NO_ASISTIO", false),
                        cita(MARTES, "CANCELADA", false), cita(MARTES, "ATENDIDA", true)),
                List.of(recordatorio("TELEGRAM", "ENVIADO", LUNES), recordatorio("TELEGRAM_REFERIDO", "ENVIADO", LUNES),
                        recordatorio("LLAMADA", "FALLIDO", null)),
                List.of(consulta(ResultadoConsulta.RESUELTA_BOT, false), consulta(ResultadoConsulta.RESUELTA_BOT, true),
                        consulta(ResultadoConsulta.ESCALADA, false), consulta(null, false)));

        var r = ActividadPeriodo.resumir(LUNES, MARTES, filas, MARTES.plusDays(1));

        assertThat(r.registros().total()).isEqualTo(3);
        assertThat(r.registros().promedioMin()).isEqualTo(4.0);
        assertThat(r.registros().minimoMin()).isEqualTo(2.0);
        assertThat(r.registros().maximoMin()).isEqualTo(6.0);

        // Canceladas no cuentan: 1 no asistio de 3 elegibles.
        assertThat(r.citas().ausentismoPct()).isEqualTo(33.33);
        assertThat(r.citas().canceladas()).isEqualTo(1);

        assertThat(r.recordatorios().enviados()).isEqualTo(2);
        assertThat(r.recordatorios().fallidos()).isEqualTo(1);
        assertThat(r.recordatorios().enviadosPorCanal()).containsEntry("TELEGRAM", 1).containsEntry("TELEGRAM_REFERIDO", 1);
        assertThat(r.recordatorios().coberturaPct()).isEqualTo(66.67);

        // Abierta no entra; la derivada no cuenta como resuelta por el asistente.
        assertThat(r.consultas().total()).isEqualTo(4);
        assertThat(r.consultas().abiertas()).isEqualTo(1);
        assertThat(r.consultas().resueltasPorAsistente()).isEqualTo(1);
        assertThat(r.consultas().derivadas()).isEqualTo(2);
        assertThat(r.consultas().atendidasAutomaticoPct()).isEqualTo(33.33);

        assertThat(r.dias()).extracting(ActividadPeriodo.Dia::fecha).containsExactly(LUNES, MARTES);
        var lunes = r.dias().get(0);
        assertThat(lunes.tprMin()).isEqualTo(5.0);
        assertThat(lunes.ausentismoPct()).isEqualTo(50.0);
        assertThat(lunes.recordatoriosEnviados()).isEqualTo(2);
    }

    @Test
    void sinEventosLosIndicadoresQuedanVacios() {
        var r = ActividadPeriodo.resumir(LUNES, MARTES,
                new ActividadPeriodo.Filas(List.of(), List.of(), List.of(), List.of()), MARTES);

        assertThat(r.registros().promedioMin()).isNull();
        assertThat(r.citas().ausentismoPct()).isNull();
        assertThat(r.consultas().atendidasAutomaticoPct()).isNull();
        assertThat(r.dias()).isEmpty();
    }

    private static ActividadPeriodo.Registro registro(LocalDate fecha, double minutos) {
        return new ActividadPeriodo.Registro("Ana Pérez", "Recepción", fecha, NUEVE, minutos, CanalMedicion.INTRANET, false);
    }

    private static ActividadPeriodo.Cita cita(LocalDate fecha, String estado, boolean recordatorio) {
        return new ActividadPeriodo.Cita("Ana Pérez", "Dra. Ruiz", fecha, NUEVE, estado, recordatorio, 0);
    }

    private static ActividadPeriodo.Recordatorio recordatorio(String canal, String estado, LocalDate enviado) {
        return new ActividadPeriodo.Recordatorio("Ana Pérez", LUNES, "DIA_ANTES", canal, estado,
                LUNES.atTime(8, 0), enviado == null ? null : enviado.atTime(8, 1), null);
    }

    private static ActividadPeriodo.Consulta consulta(ResultadoConsulta resultado, boolean derivada) {
        return new ActividadPeriodo.Consulta(null, LUNES, NUEVE, CategoriaConsulta.HORARIOS, CanalConsulta.TELEGRAM,
                resultado, derivada, false, 0.1);
    }
}

package com.threepartners.oncologia.application.dashboard;

import com.threepartners.oncologia.domain.estudio.EventoImportado;
import com.threepartners.oncologia.domain.estudio.Fase;
import com.threepartners.oncologia.domain.estudio.RecoleccionSesiones;
import com.threepartners.oncologia.domain.estudio.TipoEventoImportado;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Los eventos importados del instrumento deben dar, por sesion, el mismo valor
 * que se reporta en la tesis (redondeo hacia arriba en el medio: 9,375 -> 9,38).
 */
class ActividadDesdeEventosImportadosTest {

    private static final LocalDate VIERNES = LocalDate.of(2026, 8, 28);

    private static EventoImportado registro(int numero, double minutos) {
        return new EventoImportado(Fase.PRETEST, TipoEventoImportado.REGISTRO, numero, VIERNES, "PAC-" + numero,
                LocalTime.of(9, 0), LocalTime.of(9, 10), minutos, "Manual", null, null, null, null, null, null, null,
                null, "OK", "prueba");
    }

    private static EventoImportado cita(int numero, String estado) {
        return new EventoImportado(Fase.PRETEST, TipoEventoImportado.CITA, numero, VIERNES, "PAC-" + numero,
                null, null, null, null, estado, null, false, null, null, null, null, null, null, "prueba");
    }

    private static EventoImportado consulta(int numero, boolean resuelta, boolean derivada) {
        return new EventoImportado(Fase.PRETEST, TipoEventoImportado.CONSULTA, numero, VIERNES, "PAC-" + numero,
                null, null, null, null, null, null, null, "Citas", "Presencial", resuelta, derivada, 2.0, null, "prueba");
    }

    @Test
    void calculaCadaIndicadorDeLaSesionConLasReglasDeLaTesis() {
        var eventos = List.of(
                registro(1, 9.0), registro(2, 9.75),
                cita(1, "Asistió"), cita(2, "No asistió"), cita(3, "Cancelada"),
                consulta(1, true, false), consulta(2, true, true), consulta(3, false, true), consulta(4, true, false));

        var detalle = ActividadDesdeEventosImportados.fichas(eventos);
        var resumen = RecoleccionSesiones.resumir(Fase.PRETEST, VIERNES, VIERNES,
                EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY), detalle, VIERNES.plusDays(3));

        var sesion = resumen.sesiones().getFirst();
        assertThat(sesion.registros()).isEqualTo(2);
        assertThat(sesion.tprMin()).isEqualTo(9.38);
        // La cancelada no entra: 1 de 2 citas con desenlace
        assertThat(sesion.citasElegibles()).isEqualTo(2);
        assertThat(sesion.taPct()).isEqualTo(50.0);
        // Resuelta pero derivada no cuenta: 2 de 4
        assertThat(sesion.consultas()).isEqualTo(4);
        assertThat(sesion.resueltas()).isEqualTo(2);
        assertThat(sesion.ncaPct()).isEqualTo(50.0);
        assertThat(detalle.tiempos().getFirst().horaFin()).isEqualTo(LocalTime.of(9, 10));
    }
}

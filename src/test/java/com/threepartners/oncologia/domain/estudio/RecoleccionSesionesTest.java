package com.threepartners.oncologia.domain.estudio;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fija las definiciones operacionales de la tesis v8 sobre un escenario
 * conocido: semana del lunes 5 al domingo 11 de octubre de 2026, sesiones
 * lunes, miercoles y viernes.
 */
class RecoleccionSesionesTest {

    private static final Set<DayOfWeek> LMV = EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY);
    private static final LocalDate LUNES = LocalDate.of(2026, 10, 5);
    private static final LocalDate MARTES = LUNES.plusDays(1);
    private static final LocalDate MIERCOLES = LUNES.plusDays(2);
    private static final LocalDate VIERNES = LUNES.plusDays(4);
    private static final LocalDate DOMINGO = LUNES.plusDays(6);
    private static final LocalDate HOY = LocalDate.of(2026, 10, 12);

    private static RecoleccionSesiones.FilaTiempo tiempo(LocalDate fecha, double minutos) {
        return new RecoleccionSesiones.FilaTiempo("PAC-0001", fecha, LocalTime.of(9, 0), LocalTime.of(9, 5),
                minutos, CanalMedicion.INTRANET, false);
    }

    private static RecoleccionSesiones.FilaCita cita(LocalDate fecha, String estado) {
        return new RecoleccionSesiones.FilaCita("PAC-0001", fecha, LocalTime.of(10, 0), estado, true, 0);
    }

    private static RecoleccionSesiones.FilaConsulta consulta(LocalDate fecha, ResultadoConsulta resultado,
                                                             boolean derivada, boolean reabierta) {
        return new RecoleccionSesiones.FilaConsulta("PAC-0001", fecha, LocalTime.of(11, 0), CategoriaConsulta.CITAS,
                CanalConsulta.CHATBOT_WEB, resultado, derivada, reabierta, 0.5);
    }

    @Test
    void lasSesionesSonLosLunesMiercolesYViernesDelPeriodo() {
        assertThat(RecoleccionSesiones.fechasDeSesion(LUNES, DOMINGO, LMV)).containsExactly(LUNES, MIERCOLES, VIERNES);
        // Agosto de 2026 (pretest de la tesis): 13 sesiones.
        assertThat(RecoleccionSesiones.fechasDeSesion(LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 31), LMV)).hasSize(13);
    }

    @Test
    void calculaCadaIndicadorPorSesionConLasDefinicionesDeLaTesis() {
        var detalle = new RecoleccionSesiones.Detalle(
                List.of(tiempo(LUNES, 3), tiempo(LUNES, 4), tiempo(MARTES, 20)),
                List.of(cita(LUNES, "ATENDIDA"), cita(LUNES, "ATENDIDA"), cita(LUNES, "NO_ASISTIO"),
                        cita(LUNES, "CANCELADA")),
                List.of(consulta(LUNES, ResultadoConsulta.RESUELTA_BOT, false, false),
                        consulta(LUNES, ResultadoConsulta.RESUELTA_BOT, true, false),   // derivada y luego resuelta
                        consulta(LUNES, ResultadoConsulta.RESUELTA_BOT, false, true),   // volvio a preguntar
                        consulta(LUNES, ResultadoConsulta.ESCALADA, true, false),
                        consulta(LUNES, null, false, false)));                           // abierta

        var resumen = RecoleccionSesiones.resumir(Fase.POSTEST, LUNES, DOMINGO, LMV, detalle, HOY);
        var lunes = resumen.sesiones().getFirst();

        assertThat(lunes.registros()).isEqualTo(2);
        assertThat(lunes.tprMin()).isEqualTo(3.5);
        assertThat(lunes.citasElegibles()).isEqualTo(3);          // la cancelada no entra
        assertThat(lunes.inasistencias()).isEqualTo(1);
        assertThat(lunes.taPct()).isEqualTo(33.33);
        assertThat(lunes.consultas()).isEqualTo(4);               // la abierta queda fuera
        assertThat(lunes.resueltas()).isEqualTo(1);               // solo la del primer contacto sin derivar
        assertThat(lunes.ncaPct()).isEqualTo(25.0);
        assertThat(resumen.avisos().consultasAbiertas()).isEqualTo(1);
        assertThat(resumen.avisos().eventosFueraDeSesion()).isEqualTo(1); // el registro del martes
    }

    @Test
    void unaSesionSinEventosDejaElIndicadorVacioYNoCuentaEnElPromedio() {
        var detalle = new RecoleccionSesiones.Detalle(
                List.of(tiempo(LUNES, 3), tiempo(VIERNES, 5)), List.of(), List.of());

        var resumen = RecoleccionSesiones.resumir(Fase.POSTEST, LUNES, DOMINGO, LMV, detalle, HOY);

        assertThat(resumen.sesiones()).hasSize(3);
        assertThat(resumen.sesiones().get(1).tprMin()).isNull();   // miercoles sin registros
        assertThat(resumen.promedioSesiones().tprMin()).isEqualTo(4.0);
        assertThat(resumen.promedioSesiones().taPct()).isNull();
    }

    @Test
    void avisaLasCitasPasadasSinDesenlace() {
        var detalle = new RecoleccionSesiones.Detalle(List.of(),
                List.of(cita(LUNES, "PROGRAMADA"), cita(LUNES, "CONFIRMADA"), cita(HOY, "PROGRAMADA")), List.of());

        var resumen = RecoleccionSesiones.resumir(Fase.POSTEST, LUNES, HOY, LMV, detalle, HOY);

        assertThat(resumen.avisos().citasSinDesenlace()).isEqualTo(2);
        assertThat(resumen.avisos().hayPendientes()).isTrue();
        assertThat(resumen.sesiones().getFirst().taPct()).isNull();
    }

    @Test
    void enUnaCapturaManualDelPretestCuentaLaResueltaPorElPersonalQueLaRecibio() {
        var manual = new RecoleccionSesiones.FilaConsulta("PAC-0002", LUNES, LocalTime.NOON, CategoriaConsulta.HORARIOS,
                CanalConsulta.WHATSAPP, ResultadoConsulta.RESUELTA_PERSONAL, false, false, null);
        var chatbotAtendidaPorPersonal = new RecoleccionSesiones.FilaConsulta("PAC-0003", LUNES, LocalTime.NOON,
                CategoriaConsulta.HORARIOS, CanalConsulta.CHATBOT_WEB, ResultadoConsulta.RESUELTA_PERSONAL, false, false, null);

        assertThat(manual.resueltaEnPrimerContacto()).isTrue();
        assertThat(chatbotAtendidaPorPersonal.resueltaEnPrimerContacto()).isFalse();
        assertThat(chatbotAtendidaPorPersonal.derivadaAlPersonal()).isTrue();
    }

    @Test
    void laConsultaRecuerdaSiFueDerivadaOReabierta() {
        var ahora = java.time.Instant.parse("2026-10-05T15:00:00Z");
        Consulta derivada = Consulta.abrir(CanalConsulta.CHATBOT_WEB, "s1", null, "GENERAL_QUERY", "x", ahora, ahora);
        derivada.escalar(ahora);
        Consulta reabierta = Consulta.abrir(CanalConsulta.CHATBOT_WEB, "s2", null, "GENERAL_QUERY", "x", ahora, ahora);
        reabierta.cerrar(ResultadoConsulta.RESUELTA_BOT, null, ahora);
        reabierta.reabrirPorReformulacion(ahora);
        reabierta.cerrar(ResultadoConsulta.RESUELTA_BOT, null, ahora);

        assertThat(derivada.isDerivada()).isTrue();
        assertThat(reabierta.isReabierta()).isTrue();
        assertThat(reabierta.resueltaEnPrimerContacto()).isFalse();
    }
}

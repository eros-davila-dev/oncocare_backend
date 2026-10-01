package com.threepartners.oncologia.infrastructure.out.persistence.jpa;

import com.threepartners.oncologia.domain.cita.EstadoCita;
import com.threepartners.oncologia.domain.paciente.EstadisticasPacientes;
import com.threepartners.oncologia.domain.paciente.EstadisticasPacientesRepositoryPort;
import com.threepartners.oncologia.domain.tratamiento.EstadoCicloTratamiento;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;

/**
 * Igual que IndicadoresRepositoryAdapter: accede directamente a los
 * JpaRepository (no al puerto de dominio) porque estos numeros son
 * agregados de solo lectura que combinan varias tablas, no operaciones
 * sobre un unico agregado de dominio.
 */
@Component
@RequiredArgsConstructor
public class EstadisticasPacientesRepositoryAdapter implements EstadisticasPacientesRepositoryPort {

    private final PacienteJpaRepository pacienteJpaRepository;
    private final CitaJpaRepository citaJpaRepository;
    private final CicloTratamientoJpaRepository cicloTratamientoJpaRepository;

    @Override
    public EstadisticasPacientes calcular() {
        LocalDate hoy = LocalDate.now();

        LocalDate inicioMesActual = hoy.withDayOfMonth(1);
        LocalDate inicioMesAnterior = inicioMesActual.minusMonths(1);
        LocalDate finMesAnterior = inicioMesActual.minusDays(1);

        long pacientesRegistrados = pacienteJpaRepository.count();
        long nuevosMesActual = pacienteJpaRepository.countByFechaRegistroBetween(
                inicioInstant(inicioMesActual), Instant.now());
        long nuevosMesAnterior = pacienteJpaRepository.countByFechaRegistroBetween(
                inicioInstant(inicioMesAnterior), inicioInstant(finMesAnterior.plusDays(1)));
        Double variacionPacientes = variacion(nuevosMesActual, nuevosMesAnterior);

        long pacientesEnTratamiento = pacienteJpaRepository.contarEnTratamiento();
        long sesionesProgramadasMesActual = cicloTratamientoJpaRepository.countByEstadoAndFechaSesionBetween(
                EstadoCicloTratamiento.PROGRAMADO, inicioMesActual, hoy);
        long sesionesProgramadasMesAnterior = cicloTratamientoJpaRepository.countByEstadoAndFechaSesionBetween(
                EstadoCicloTratamiento.PROGRAMADO, inicioMesAnterior, finMesAnterior);
        Double variacionEnTratamiento = variacion(sesionesProgramadasMesActual, sesionesProgramadasMesAnterior);

        LocalDate inicioSemanaActual = hoy.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate finSemanaActual = inicioSemanaActual.plusDays(6);
        LocalDate inicioSemanaAnterior = inicioSemanaActual.minusWeeks(1);
        LocalDate finSemanaAnterior = finSemanaActual.minusWeeks(1);

        long citasEstaSemana = citaJpaRepository.countByFechaBetween(inicioSemanaActual, finSemanaActual);
        long citasSemanaAnterior = citaJpaRepository.countByFechaBetween(inicioSemanaAnterior, finSemanaAnterior);
        Double variacionCitas = variacion(citasEstaSemana, citasSemanaAnterior);

        double asistenciaActual = asistencia(hoy.minusDays(30), hoy);
        double asistenciaAnterior = asistencia(hoy.minusDays(60), hoy.minusDays(31));
        Double variacionAsistencia = asistenciaAnterior == 0.0 ? null : asistenciaActual - asistenciaAnterior;

        return new EstadisticasPacientes(
                pacientesRegistrados, variacionPacientes,
                pacientesEnTratamiento, variacionEnTratamiento,
                citasEstaSemana, variacionCitas,
                asistenciaActual, variacionAsistencia);
    }

    private double asistencia(LocalDate desde, LocalDate hasta) {
        long atendidas = citaJpaRepository.countByEstadoAndFechaBetween(EstadoCita.ATENDIDA, desde, hasta);
        long noAsistio = citaJpaRepository.countByEstadoAndFechaBetween(EstadoCita.NO_ASISTIO, desde, hasta);
        long total = atendidas + noAsistio;
        return total == 0 ? 0.0 : (atendidas * 100.0) / total;
    }

    private static Instant inicioInstant(LocalDate fecha) {
        return fecha.atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    private static Double variacion(long actual, long anterior) {
        if (anterior == 0) {
            return actual == 0 ? 0.0 : null;
        }
        return ((actual - anterior) * 100.0) / anterior;
    }
}

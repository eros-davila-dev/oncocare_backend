package com.threepartners.oncologia.domain.cita;

import com.threepartners.oncologia.domain.shared.exception.ValidacionDeNegocioException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Cita {

    private Long id;
    private Long pacienteId;
    private Long medicoId;
    private LocalDate fecha;
    private LocalTime hora;
    private String tipoConsulta;
    private EstadoCita estado;
    private String observaciones;
    private OrigenCita origen;
    private Instant fechaCreacion;
    /** Momento en que se registro ATENDIDA / NO_ASISTIO: trazabilidad del indicador TNS. */
    private Instant fechaHoraDesenlace;
    private Long desenlaceRegistradoPor;
    /** NO_ASISTIO puesto por el job de cierre (nadie registro el desenlace a tiempo). */
    private boolean cierreAutomatico;
    private int vecesReprogramada;

    /**
     * Cita del pretest transcrita por el investigador desde las hojas de
     * calculo de la fundacion (ficha de registro de ausentismo). Nace ya con su
     * desenlace y no participa de recordatorios ni de la agenda.
     */
    public static Cita capturaPretest(Long pacienteId, LocalDate fecha, LocalTime hora, String tipoConsulta,
                                      boolean asistio, Long investigadorId, Instant ahora) {
        return Cita.builder()
                .pacienteId(pacienteId)
                .fecha(fecha)
                .hora(hora != null ? hora : LocalTime.MIDNIGHT)
                .tipoConsulta(tipoConsulta)
                .estado(asistio ? EstadoCita.ATENDIDA : EstadoCita.NO_ASISTIO)
                .origen(OrigenCita.CAPTURA_PRETEST)
                .fechaCreacion(ahora)
                .fechaHoraDesenlace(ahora)
                .desenlaceRegistradoPor(investigadorId)
                .build();
    }

    public LocalDateTime fechaHora() {
        return LocalDateTime.of(fecha, hora);
    }

    public boolean tieneDesenlace() {
        return estado == EstadoCita.ATENDIDA || estado == EstadoCita.NO_ASISTIO;
    }

    public boolean esFinal() {
        return tieneDesenlace() || estado == EstadoCita.CANCELADA;
    }

    public void confirmar() {
        validarTransicion(EstadoCita.CONFIRMADA);
        this.estado = EstadoCita.CONFIRMADA;
    }

    public void atender(Long registradoPor, Instant cuando) {
        validarTransicion(EstadoCita.ATENDIDA);
        this.estado = EstadoCita.ATENDIDA;
        registrarDesenlace(registradoPor, cuando, false);
    }

    public void cancelar() {
        validarTransicion(EstadoCita.CANCELADA);
        this.estado = EstadoCita.CANCELADA;
    }

    public void marcarNoAsistio(Long registradoPor, Instant cuando, boolean automatico) {
        validarTransicion(EstadoCita.NO_ASISTIO);
        this.estado = EstadoCita.NO_ASISTIO;
        registrarDesenlace(registradoPor, cuando, automatico);
    }

    /**
     * Reprogramar mueve la misma cita (conserva su historial y cuenta una
     * sola vez para el TNS, con su desenlace final). Una cita ya atendida, con
     * inasistencia o cancelada no puede reprogramarse: hacerlo borraria su
     * desenlace y alteraria el indicador.
     */
    public void reprogramar(LocalDate nuevaFecha, LocalTime nuevaHora) {
        if (esFinal()) {
            throw new ValidacionDeNegocioException(
                    "No se puede reprogramar una cita en estado final: " + estado + ". Agende una nueva cita.");
        }
        this.fecha = nuevaFecha;
        this.hora = nuevaHora;
        this.estado = EstadoCita.PROGRAMADA;
        this.vecesReprogramada++;
    }

    /**
     * El desenlace (atendida / no asistio) describe algo que ya ocurrio: no
     * puede registrarse antes del dia de la cita.
     */
    public void exigirFechaAlcanzada(LocalDate hoy) {
        if (fecha.isAfter(hoy)) {
            throw new ValidacionDeNegocioException(
                    "La cita es del " + fecha + ": su asistencia solo puede registrarse desde ese dia");
        }
    }

    /**
     * Corrige un desenlace mal registrado (p. ej. se marco "no asistio" a un
     * paciente que si llego). Solo entre los dos estados de desenlace; el
     * motivo y el valor previo se guardan como correccion auditada.
     */
    public void corregirDesenlace(EstadoCita nuevoEstado, Long registradoPor, Instant cuando) {
        if (!tieneDesenlace()) {
            throw new ValidacionDeNegocioException("La cita aun no tiene desenlace registrado: no hay nada que corregir");
        }
        if (nuevoEstado != EstadoCita.ATENDIDA && nuevoEstado != EstadoCita.NO_ASISTIO) {
            throw new ValidacionDeNegocioException("El desenlace solo puede corregirse a ATENDIDA o NO_ASISTIO");
        }
        if (nuevoEstado == estado) {
            throw new ValidacionDeNegocioException("La cita ya tiene el desenlace " + estado);
        }
        this.estado = nuevoEstado;
        registrarDesenlace(registradoPor, cuando, false);
    }

    private void registrarDesenlace(Long registradoPor, Instant cuando, boolean automatico) {
        this.fechaHoraDesenlace = cuando;
        this.desenlaceRegistradoPor = registradoPor;
        this.cierreAutomatico = automatico;
    }

    private void validarTransicion(EstadoCita destino) {
        if (esFinal()) {
            throw new ValidacionDeNegocioException(
                    "No se puede cambiar el estado de una cita en estado final: " + this.estado);
        }
        if (destino == EstadoCita.PROGRAMADA) {
            throw new ValidacionDeNegocioException("No es posible retroceder una cita a estado PROGRAMADA");
        }
    }
}

package com.threepartners.oncologia.domain.cita;

import com.threepartners.oncologia.domain.shared.exception.ValidacionDeNegocioException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    public LocalDateTime fechaHora() {
        return LocalDateTime.of(fecha, hora);
    }

    public void confirmar() {
        validarTransicion(EstadoCita.CONFIRMADA);
        this.estado = EstadoCita.CONFIRMADA;
    }

    public void atender() {
        validarTransicion(EstadoCita.ATENDIDA);
        this.estado = EstadoCita.ATENDIDA;
    }

    public void cancelar() {
        validarTransicion(EstadoCita.CANCELADA);
        this.estado = EstadoCita.CANCELADA;
    }

    public void marcarNoAsistio() {
        validarTransicion(EstadoCita.NO_ASISTIO);
        this.estado = EstadoCita.NO_ASISTIO;
    }

    private void validarTransicion(EstadoCita destino) {
        if (this.estado == EstadoCita.CANCELADA || this.estado == EstadoCita.ATENDIDA || this.estado == EstadoCita.NO_ASISTIO) {
            throw new ValidacionDeNegocioException(
                    "No se puede cambiar el estado de una cita en estado final: " + this.estado);
        }
        if (destino == EstadoCita.PROGRAMADA) {
            throw new ValidacionDeNegocioException("No es posible retroceder una cita a estado PROGRAMADA");
        }
    }
}

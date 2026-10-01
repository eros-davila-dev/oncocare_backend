package com.threepartners.oncologia.domain.estudio;

import com.threepartners.oncologia.domain.shared.exception.ValidacionDeNegocioException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Periodo de una fase del estudio. La pertenencia de cualquier evento
 * (registro, cita, consulta) a una fase se decide por su fecha y estas fechas,
 * nunca por una etiqueta guardada en el evento: asi no hay dos fuentes de
 * verdad. Cerrar la fase congela sus fechas y, por tanto, sus resultados.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FaseEstudio {

    private Long id;
    private Fase fase;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private EstadoFase estado;

    public boolean contiene(LocalDate fecha) {
        return fecha != null && !fecha.isBefore(fechaInicio) && !fecha.isAfter(fechaFin);
    }

    public boolean estaCerrada() {
        return estado == EstadoFase.CERRADA;
    }

    public PeriodoMedicion periodo() {
        return new PeriodoMedicion(fechaInicio, fechaFin);
    }

    public void configurarFechas(LocalDate inicio, LocalDate fin) {
        if (estaCerrada()) {
            throw new ValidacionDeNegocioException(
                    "La fase " + fase + " esta cerrada: sus fechas ya no pueden modificarse");
        }
        if (inicio == null || fin == null || fin.isBefore(inicio)) {
            throw new ValidacionDeNegocioException("La fecha de fin de la fase no puede ser anterior a la de inicio");
        }
        this.fechaInicio = inicio;
        this.fechaFin = fin;
    }

    public void cerrar() {
        if (estaCerrada()) {
            throw new ValidacionDeNegocioException("La fase " + fase + " ya esta cerrada");
        }
        this.estado = EstadoFase.CERRADA;
    }
}

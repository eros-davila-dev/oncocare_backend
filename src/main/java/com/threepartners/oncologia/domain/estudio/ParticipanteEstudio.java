package com.threepartners.oncologia.domain.estudio;

import com.threepartners.oncologia.domain.shared.exception.ValidacionDeNegocioException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Paciente que forma parte de la muestra (consentimiento informado firmado).
 * Excluirlo no borra ninguno de sus datos: solo deja de contar en el alcance
 * MUESTRA y en la tabla pareada.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipanteEstudio {

    private Long id;
    private Long pacienteId;
    private String codigo;
    private LocalDate fechaConsentimiento;
    private boolean incluido;
    private MotivoExclusion motivoExclusion;
    private String observacion;
    private Instant fechaInclusion;
    private Instant fechaExclusion;

    public void excluir(MotivoExclusion motivo, String observacion, Instant cuando) {
        if (!incluido) {
            throw new ValidacionDeNegocioException("El participante " + codigo + " ya esta excluido del estudio");
        }
        if (motivo == null) {
            throw new ValidacionDeNegocioException("Debe indicar el motivo de exclusion");
        }
        this.incluido = false;
        this.motivoExclusion = motivo;
        this.observacion = observacion;
        this.fechaExclusion = cuando;
    }

    public void reincorporar() {
        if (incluido) {
            throw new ValidacionDeNegocioException("El participante " + codigo + " ya esta incluido en el estudio");
        }
        this.incluido = true;
        this.motivoExclusion = null;
        this.fechaExclusion = null;
    }

    /**
     * Codigos correlativos P01, P02... (P100 en adelante si la muestra creciera).
     */
    public static String codigoParaNumero(int numero) {
        return "P%02d".formatted(numero);
    }
}

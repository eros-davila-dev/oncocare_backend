package com.threepartners.oncologia.domain.tratamiento;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CicloTratamiento {

    private Long id;
    private Long pacienteId;
    private TipoTratamiento tipoTratamiento;
    private int numeroSesion;
    private int totalSesionesEsquema;
    private LocalDate fechaSesion;
    private Long medicoResponsableId;
    private EstadoCicloTratamiento estado;
    private String observaciones;

    public double porcentajeCumplimiento() {
        if (totalSesionesEsquema <= 0) {
            return 0.0;
        }
        return Math.min(100.0, (numeroSesion * 100.0) / totalSesionesEsquema);
    }
}

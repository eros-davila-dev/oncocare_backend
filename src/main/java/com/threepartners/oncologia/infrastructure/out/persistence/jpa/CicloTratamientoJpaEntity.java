package com.threepartners.oncologia.infrastructure.out.persistence.jpa;

import com.threepartners.oncologia.domain.tratamiento.EstadoCicloTratamiento;
import com.threepartners.oncologia.domain.tratamiento.TipoTratamiento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "ciclo_tratamiento")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CicloTratamientoJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "paciente_id", nullable = false)
    private Long pacienteId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_tratamiento", nullable = false, length = 25)
    private TipoTratamiento tipoTratamiento;

    @Column(name = "numero_sesion", nullable = false)
    private int numeroSesion;

    @Column(name = "total_sesiones_esquema", nullable = false)
    private int totalSesionesEsquema;

    @Column(name = "fecha_sesion", nullable = false)
    private LocalDate fechaSesion;

    @Column(name = "medico_responsable_id", nullable = false)
    private Long medicoResponsableId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoCicloTratamiento estado;

    @Column(columnDefinition = "TEXT")
    private String observaciones;
}

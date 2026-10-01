package com.threepartners.oncologia.infrastructure.out.persistence.jpa;

import com.threepartners.oncologia.domain.estudio.MotivoExclusion;
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

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "participante_estudio")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipanteEstudioJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "paciente_id", nullable = false, unique = true)
    private Long pacienteId;

    @Column(nullable = false, unique = true, length = 10)
    private String codigo;

    @Column(name = "fecha_consentimiento", nullable = false)
    private LocalDate fechaConsentimiento;

    @Column(nullable = false)
    private boolean incluido;

    @Enumerated(EnumType.STRING)
    @Column(name = "motivo_exclusion", length = 30)
    private MotivoExclusion motivoExclusion;

    @Column(columnDefinition = "TEXT")
    private String observacion;

    @Column(name = "fecha_inclusion", nullable = false)
    private Instant fechaInclusion;

    @Column(name = "fecha_exclusion")
    private Instant fechaExclusion;
}

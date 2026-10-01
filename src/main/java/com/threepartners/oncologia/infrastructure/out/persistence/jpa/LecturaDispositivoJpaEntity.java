package com.threepartners.oncologia.infrastructure.out.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

@Entity
@Table(name = "lectura_dispositivo")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LecturaDispositivoJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dispositivo_id", nullable = false)
    private Long dispositivoId;

    @Column(name = "paciente_id")
    private Long pacienteId;

    @Column(name = "ciclo_tratamiento_id")
    private Long cicloTratamientoId;

    @Column(name = "tipo_dato", nullable = false)
    private String tipoDato;

    @Column(nullable = false)
    private String valor;

    @Column(name = "fecha_lectura", nullable = false)
    private Instant fechaLectura;
}

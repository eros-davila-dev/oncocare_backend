package com.threepartners.oncologia.infrastructure.out.persistence.jpa;

import com.threepartners.oncologia.domain.estudio.CanalMedicion;
import com.threepartners.oncologia.domain.estudio.EstadoMedicion;
import com.threepartners.oncologia.domain.estudio.TipoMedicion;
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

@Entity
@Table(name = "medicion_registro")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicionRegistroJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoMedicion tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CanalMedicion canal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private EstadoMedicion estado;

    @Column(nullable = false)
    private boolean sospechosa;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "paciente_id")
    private Long pacienteId;

    @Column(name = "entidad_id")
    private Long entidadId;

    @Column(nullable = false)
    private Instant inicio;

    private Instant fin;

    /** Columna generada por PostgreSQL a partir de inicio/fin: solo lectura. */
    @Column(name = "duracion_segundos", insertable = false, updatable = false)
    private Integer duracionSegundos;

    @Column(name = "capturado_por")
    private Long capturadoPor;

    @Column(columnDefinition = "TEXT")
    private String observacion;
}

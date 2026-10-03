package com.threepartners.oncologia.infrastructure.out.persistence.jpa;

import com.threepartners.oncologia.domain.estudio.CategoriaConsulta;

import com.threepartners.oncologia.domain.estudio.CanalConsulta;
import com.threepartners.oncologia.domain.estudio.ResultadoConsulta;
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
@Table(name = "consulta")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsultaJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CanalConsulta canal;

    @Column(name = "sesion_id", length = 100)
    private String sesionId;

    @Column(name = "paciente_id")
    private Long pacienteId;

    @Column(length = 40)
    private String intencion;

    @Column(length = 300)
    private String resumen;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ResultadoConsulta resultado;

    @Column(name = "abierta_en", nullable = false)
    private Instant abiertaEn;

    @Column(name = "cerrada_en")
    private Instant cerradaEn;

    @Column(name = "tiempo_primera_respuesta_ms")
    private Integer tiempoPrimeraRespuestaMs;

    private Short valoracion;

    @Column(name = "resuelta_por_usuario_id")
    private Long resueltaPorUsuarioId;

    @Column(name = "capturado_por")
    private Long capturadoPor;

    @Column(columnDefinition = "TEXT")
    private String observacion;

    @Column(nullable = false)
    private int turnos;

    @Column(name = "ultima_actividad_en", nullable = false)
    private Instant ultimaActividadEn;

    @Column(name = "nota_resolucion", columnDefinition = "TEXT")
    private String notaResolucion;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private CategoriaConsulta categoria;

    @Column(nullable = false)
    private boolean derivada;

    @Column(nullable = false)
    private boolean reabierta;
}

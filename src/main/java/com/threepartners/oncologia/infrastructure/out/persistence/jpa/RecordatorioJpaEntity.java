package com.threepartners.oncologia.infrastructure.out.persistence.jpa;

import com.threepartners.oncologia.domain.recordatorio.CanalRecordatorio;
import com.threepartners.oncologia.domain.recordatorio.EstadoRecordatorio;
import com.threepartners.oncologia.domain.recordatorio.RespuestaRecordatorio;
import com.threepartners.oncologia.domain.recordatorio.TipoRecordatorio;
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
@Table(name = "recordatorio")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecordatorioJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cita_id", nullable = false)
    private Long citaId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoRecordatorio tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private CanalRecordatorio canal;

    @Column(name = "programado_para", nullable = false)
    private Instant programadoPara;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private EstadoRecordatorio estado;

    @Column(nullable = false)
    private int intentos;

    @Column(name = "mensaje_externo_id", length = 50)
    private String mensajeExternoId;

    @Column(name = "tomado_en")
    private Instant tomadoEn;

    @Column(name = "enviado_en")
    private Instant enviadoEn;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private RespuestaRecordatorio respuesta;

    @Column(name = "respondido_en")
    private Instant respondidoEn;

    @Column(columnDefinition = "TEXT")
    private String error;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;
}

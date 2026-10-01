package com.threepartners.oncologia.infrastructure.out.persistence.jpa;

import com.threepartners.oncologia.domain.auditoria.ResultadoAuditoria;
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

import java.time.Instant;

/**
 * Mapea la tabla auditoria_accion, de solo insercion: no se exponen metodos
 * de actualizacion ni borrado en el repositorio ni en el adaptador (seccion 13).
 */
@Entity
@Table(name = "auditoria_accion")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditoriaJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(nullable = false, length = 60)
    private String accion;

    @Column(name = "entidad_afectada", nullable = false, length = 40)
    private String entidadAfectada;

    @Column(name = "entidad_id", length = 60)
    private String entidadId;

    @Column(name = "valores_previos", columnDefinition = "TEXT")
    private String valoresPrevios;

    @Column(name = "valores_nuevos", columnDefinition = "TEXT")
    private String valoresNuevos;

    @Column(name = "ip_origen", length = 45)
    private String ipOrigen;

    @Column(nullable = false)
    private Instant fecha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ResultadoAuditoria resultado;
}

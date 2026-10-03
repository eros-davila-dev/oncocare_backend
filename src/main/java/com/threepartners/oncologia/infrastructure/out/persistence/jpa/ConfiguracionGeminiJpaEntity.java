package com.threepartners.oncologia.infrastructure.out.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "configuracion_gemini")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfiguracionGeminiJpaEntity {

    /** Fila unica: la tabla tiene CHECK (id = 1). */
    public static final short ID_UNICO = 1;

    @Id
    private Short id;

    @Column(name = "api_key_cifrada", columnDefinition = "TEXT")
    private String apiKeyCifrada;

    /** Separados por coma, en orden de preferencia. */
    @Column(nullable = false, length = 1000)
    private String modelos;

    @Column(name = "actualizado_por")
    private Long actualizadoPor;

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;
}

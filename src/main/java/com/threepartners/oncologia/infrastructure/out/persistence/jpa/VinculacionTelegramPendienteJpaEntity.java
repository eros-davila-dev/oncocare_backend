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
@Table(name = "telegram_vinculacion_pendiente")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VinculacionTelegramPendienteJpaEntity {

    @Id
    @Column(name = "chat_id")
    private Long chatId;

    /** Ids de paciente separados por coma. */
    @Column(nullable = false, length = 200)
    private String titulares;

    @Column(nullable = false, length = 200)
    private String referidos;

    @Column(nullable = false)
    private int intentos;

    @Column(name = "expira_en", nullable = false)
    private Instant expiraEn;

    @Column(name = "bloqueado_hasta")
    private Instant bloqueadoHasta;
}

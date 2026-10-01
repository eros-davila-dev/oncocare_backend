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
@Table(name = "conversacion_chatbot")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversacionChatbotJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "paciente_id")
    private Long pacienteId;

    @Column(name = "sesion_id", length = 100)
    private String sesionId;

    @Column(name = "consulta_id")
    private Long consultaId;

    @Column(name = "mensaje_usuario", columnDefinition = "TEXT", nullable = false)
    private String mensajeUsuario;

    @Column(name = "respuesta_bot", columnDefinition = "TEXT")
    private String respuestaBot;

    @Column(name = "intencion_detectada", length = 60)
    private String intencionDetectada;

    @Column(nullable = false)
    private Instant fecha;

    @Column(length = 20)
    private String canal;
}

package com.threepartners.oncologia.infrastructure.out.persistence.jpa;

import com.threepartners.oncologia.domain.paciente.ConvenioSeguro;
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
@Table(name = "paciente")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PacienteJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", unique = true)
    private Long usuarioId;

    @Column(nullable = false)
    private String nombres;

    @Column(nullable = false)
    private String apellidos;

    @Column(name = "documento_identidad", nullable = false, unique = true)
    private String documentoIdentidad;

    @Column(name = "fecha_nacimiento", nullable = false)
    private LocalDate fechaNacimiento;

    @Column
    private String telefono;

    @Column
    private String email;

    @Column
    private String direccion;

    @Column(name = "tipo_cancer")
    private String tipoCancer;

    @Column(name = "estadio_clinico")
    private String estadioClinico;

    @Column(name = "fecha_diagnostico")
    private LocalDate fechaDiagnostico;

    @Column(name = "medico_tratante_id")
    private Long medicoTratanteId;

    @Enumerated(EnumType.STRING)
    @Column(name = "convenio_seguro", length = 20)
    private ConvenioSeguro convenioSeguro;

    @Column(name = "contacto_emergencia_nombre")
    private String contactoEmergenciaNombre;

    @Column(name = "contacto_emergencia_telefono")
    private String contactoEmergenciaTelefono;

    @Column(nullable = false)
    private boolean activo;

    @Column(name = "fecha_registro", nullable = false)
    private Instant fechaRegistro;

}

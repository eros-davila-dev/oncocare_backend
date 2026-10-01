package com.threepartners.oncologia.domain.paciente;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Paciente {

    private Long id;
    private Long usuarioId;
    private String nombres;
    private String apellidos;
    private String documentoIdentidad;
    private LocalDate fechaNacimiento;
    private String telefono;
    private String email;
    private String direccion;
    private String tipoCancer;
    private String estadioClinico;
    private LocalDate fechaDiagnostico;
    private Long medicoTratanteId;
    private ConvenioSeguro convenioSeguro;
    private String contactoEmergenciaNombre;
    private String contactoEmergenciaTelefono;
    private boolean activo;
    private Instant fechaRegistro;
    private Integer tiempoRegistroSegundos;

    public int edad() {
        if (fechaNacimiento == null) {
            return 0;
        }
        return Period.between(fechaNacimiento, LocalDate.now()).getYears();
    }

    public String nombreCompleto() {
        return "%s %s".formatted(nombres, apellidos);
    }
}

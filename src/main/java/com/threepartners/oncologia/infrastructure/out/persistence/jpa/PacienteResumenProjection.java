package com.threepartners.oncologia.infrastructure.out.persistence.jpa;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Proyeccion de la query nativa de PacienteJpaRepository#buscarResumen.
 * Los nombres de metodo se resuelven contra los alias de columna en SQL
 * (snake_case -> camelCase, resolucion relajada de Spring Data).
 */
public interface PacienteResumenProjection {

    Long getId();

    String getNombres();

    String getApellidos();

    String getDocumentoIdentidad();

    LocalDate getFechaNacimiento();

    String getTelefono();

    String getEmail();

    String getDireccion();

    String getTipoCancer();

    String getEstadioClinico();

    LocalDate getFechaDiagnostico();

    Long getMedicoTratanteId();

    String getConvenioSeguro();

    String getContactoEmergenciaNombre();

    String getContactoEmergenciaTelefono();

    Boolean getActivo();

    Instant getFechaRegistro();

    Integer getTiempoRegistroSegundos();

    String getMedicoNombre();

    String getMedicoEspecialidad();

    LocalDate getUltimaCita();

    LocalDate getProximaCita();

    String getEstadoTratamiento();
}

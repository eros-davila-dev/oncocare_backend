package com.threepartners.oncologia.infrastructure.out.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ParticipanteEstudioJpaRepository extends JpaRepository<ParticipanteEstudioJpaEntity, Long> {

    Optional<ParticipanteEstudioJpaEntity> findByPacienteId(Long pacienteId);

    boolean existsByPacienteId(Long pacienteId);

    Optional<ParticipanteEstudioJpaEntity> findByCodigoIgnoreCase(String codigo);

    @Query("""
            SELECT pe, p.nombres, p.apellidos, p.documentoIdentidad
            FROM ParticipanteEstudioJpaEntity pe, PacienteJpaEntity p
            WHERE p.id = pe.pacienteId
            ORDER BY pe.codigo
            """)
    List<Object[]> listarConPaciente();

    /**
     * Codigos con formato Pnn: se extrae la parte numerica para asignar el
     * siguiente correlativo sin depender del orden alfabetico (P100 > P99).
     */
    @Query(value = """
            SELECT COALESCE(MAX(CAST(SUBSTRING(codigo FROM 2) AS INTEGER)), 0)
            FROM participante_estudio
            WHERE codigo ~ '^P[0-9]+$'
            """, nativeQuery = true)
    int maximoNumeroCodigo();
}

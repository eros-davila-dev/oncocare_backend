package com.threepartners.oncologia.infrastructure.out.persistence.jpa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface PacienteJpaRepository extends JpaRepository<PacienteJpaEntity, Long> {

    Optional<PacienteJpaEntity> findByDocumentoIdentidad(String documentoIdentidad);

    boolean existsByDocumentoIdentidad(String documentoIdentidad);

    boolean existsByDocumentoIdentidadAndIdNot(String documentoIdentidad, Long id);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    Optional<PacienteJpaEntity> findByUsuarioId(Long usuarioId);

    Optional<PacienteJpaEntity> findByTelegramChatId(Long telegramChatId);

    long countByFechaRegistroBetween(Instant desde, Instant hasta);

    @Query("""
            SELECT p FROM PacienteJpaEntity p
            WHERE :texto IS NULL OR :texto = ''
               OR LOWER(p.nombres) LIKE LOWER(CONCAT('%', :texto, '%'))
               OR LOWER(p.apellidos) LIKE LOWER(CONCAT('%', :texto, '%'))
               OR p.documentoIdentidad LIKE CONCAT('%', :texto, '%')
            """)
    Page<PacienteJpaEntity> buscar(@Param("texto") String texto, Pageable pageable);

    /**
     * "En tratamiento" = tiene al menos un ciclo PROGRAMADO (sesiones
     * pendientes) y su ciclo mas reciente (por fecha_sesion) no esta
     * SUSPENDIDO. Misma regla usada en la CASE de buscarResumen, para que el
     * conteo agregado sea consistente con el estado mostrado por fila.
     */
    @Query(value = """
            SELECT COUNT(*) FROM paciente p
            WHERE EXISTS (SELECT 1 FROM ciclo_tratamiento ct WHERE ct.paciente_id = p.id AND ct.estado = 'PROGRAMADO')
              AND COALESCE(
                    (SELECT ct2.estado FROM ciclo_tratamiento ct2
                     WHERE ct2.paciente_id = p.id
                     ORDER BY ct2.fecha_sesion DESC, ct2.id DESC LIMIT 1),
                    '') <> 'SUSPENDIDO'
            """, nativeQuery = true)
    long contarEnTratamiento();

    @Query(value = """
            WITH base AS (
                SELECT
                    p.id AS id,
                    p.nombres AS nombres,
                    p.apellidos AS apellidos,
                    p.documento_identidad AS documento_identidad,
                    p.fecha_nacimiento AS fecha_nacimiento,
                    p.telefono AS telefono,
                    p.email AS email,
                    p.direccion AS direccion,
                    p.tipo_cancer AS tipo_cancer,
                    p.estadio_clinico AS estadio_clinico,
                    p.fecha_diagnostico AS fecha_diagnostico,
                    p.medico_tratante_id AS medico_tratante_id,
                    p.convenio_seguro AS convenio_seguro,
                    p.contacto_emergencia_nombre AS contacto_emergencia_nombre,
                    p.contacto_emergencia_telefono AS contacto_emergencia_telefono,
                    p.activo AS activo,
                    p.fecha_registro AS fecha_registro,
                    u.nombres AS medico_nombre,
                    u.especialidad AS medico_especialidad,
                    (SELECT MAX(c.fecha) FROM cita c WHERE c.paciente_id = p.id AND c.estado = 'ATENDIDA') AS ultima_cita,
                    (SELECT MIN(c.fecha) FROM cita c WHERE c.paciente_id = p.id AND c.fecha >= CURRENT_DATE AND c.estado IN ('PROGRAMADA', 'CONFIRMADA')) AS proxima_cita,
                    CASE
                        WHEN NOT EXISTS (SELECT 1 FROM ciclo_tratamiento ct WHERE ct.paciente_id = p.id) THEN 'PENDIENTE'
                        WHEN (SELECT ct2.estado FROM ciclo_tratamiento ct2 WHERE ct2.paciente_id = p.id ORDER BY ct2.fecha_sesion DESC, ct2.id DESC LIMIT 1) = 'SUSPENDIDO' THEN 'SUSPENDIDO'
                        WHEN EXISTS (SELECT 1 FROM ciclo_tratamiento ct3 WHERE ct3.paciente_id = p.id AND ct3.estado = 'PROGRAMADO') THEN 'EN_TRATAMIENTO'
                        ELSE 'FINALIZADO'
                    END AS estado_tratamiento
                FROM paciente p
                LEFT JOIN usuario u ON u.id = p.medico_tratante_id
            )
            SELECT * FROM base
            WHERE (CAST(:texto AS text) IS NULL OR CAST(:texto AS text) = ''
                   OR LOWER(nombres) LIKE LOWER(CONCAT('%', CAST(:texto AS text), '%'))
                   OR LOWER(apellidos) LIKE LOWER(CONCAT('%', CAST(:texto AS text), '%'))
                   OR documento_identidad LIKE CONCAT('%', CAST(:texto AS text), '%'))
              AND (CAST(:estado AS text) IS NULL OR estado_tratamiento = CAST(:estado AS text))
            """,
            countQuery = """
            WITH base AS (
                SELECT
                    p.id AS id,
                    p.nombres AS nombres,
                    p.apellidos AS apellidos,
                    p.documento_identidad AS documento_identidad,
                    CASE
                        WHEN NOT EXISTS (SELECT 1 FROM ciclo_tratamiento ct WHERE ct.paciente_id = p.id) THEN 'PENDIENTE'
                        WHEN (SELECT ct2.estado FROM ciclo_tratamiento ct2 WHERE ct2.paciente_id = p.id ORDER BY ct2.fecha_sesion DESC, ct2.id DESC LIMIT 1) = 'SUSPENDIDO' THEN 'SUSPENDIDO'
                        WHEN EXISTS (SELECT 1 FROM ciclo_tratamiento ct3 WHERE ct3.paciente_id = p.id AND ct3.estado = 'PROGRAMADO') THEN 'EN_TRATAMIENTO'
                        ELSE 'FINALIZADO'
                    END AS estado_tratamiento
                FROM paciente p
            )
            SELECT COUNT(*) FROM base
            WHERE (CAST(:texto AS text) IS NULL OR CAST(:texto AS text) = ''
                   OR LOWER(nombres) LIKE LOWER(CONCAT('%', CAST(:texto AS text), '%'))
                   OR LOWER(apellidos) LIKE LOWER(CONCAT('%', CAST(:texto AS text), '%'))
                   OR documento_identidad LIKE CONCAT('%', CAST(:texto AS text), '%'))
              AND (CAST(:estado AS text) IS NULL OR estado_tratamiento = CAST(:estado AS text))
            """,
            nativeQuery = true)
    Page<PacienteResumenProjection> buscarResumen(@Param("texto") String texto, @Param("estado") String estado, Pageable pageable);
}

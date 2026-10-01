package com.threepartners.oncologia.infrastructure.out.persistence.jpa;

import com.threepartners.oncologia.domain.cita.EstadoCita;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public interface CitaJpaRepository extends JpaRepository<CitaJpaEntity, Long> {

    @Query("""
            SELECT COUNT(c) > 0 FROM CitaJpaEntity c
            WHERE c.medicoId = :medicoId AND c.fecha = :fecha AND c.hora = :hora
              AND c.estado NOT IN (com.threepartners.oncologia.domain.cita.EstadoCita.CANCELADA,
                                    com.threepartners.oncologia.domain.cita.EstadoCita.NO_ASISTIO)
              AND (:idExcluido IS NULL OR c.id <> :idExcluido)
            """)
    boolean existeSolapamiento(@Param("medicoId") Long medicoId, @Param("fecha") LocalDate fecha,
                                @Param("hora") LocalTime hora, @Param("idExcluido") Long idExcluido);

    @Query("""
            SELECT c FROM CitaJpaEntity c
            WHERE (:pacienteId IS NULL OR c.pacienteId = :pacienteId)
              AND (:medicoId IS NULL OR c.medicoId = :medicoId)
              AND (:desde IS NULL OR c.fecha >= :desde)
              AND (:hasta IS NULL OR c.fecha <= :hasta)
              AND (:estado IS NULL OR c.estado = :estado)
            """)
    Page<CitaJpaEntity> filtrar(@Param("pacienteId") Long pacienteId, @Param("medicoId") Long medicoId,
                                 @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta,
                                 @Param("estado") EstadoCita estado, Pageable pageable);

    @Query(value = """
            SELECT * FROM cita c
            WHERE c.estado IN ('PROGRAMADA', 'CONFIRMADA')
              AND (c.fecha + c.hora) BETWEEN :desde AND :hasta
            """, nativeQuery = true)
    List<CitaJpaEntity> listarProximasEnVentana(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    long countByEstadoAndFechaBetween(EstadoCita estado, LocalDate desde, LocalDate hasta);

    long countByFechaBetween(LocalDate desde, LocalDate hasta);
}

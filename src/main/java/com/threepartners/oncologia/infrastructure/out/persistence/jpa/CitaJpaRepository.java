package com.threepartners.oncologia.infrastructure.out.persistence.jpa;

import com.threepartners.oncologia.domain.cita.EstadoCita;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public interface CitaJpaRepository extends JpaRepository<CitaJpaEntity, Long>,
        JpaSpecificationExecutor<CitaJpaEntity> {

    @Query("""
            SELECT COUNT(c) > 0 FROM CitaJpaEntity c
            WHERE c.medicoId = :medicoId AND c.fecha = :fecha AND c.hora = :hora
              AND c.estado NOT IN (com.threepartners.oncologia.domain.cita.EstadoCita.CANCELADA,
                                    com.threepartners.oncologia.domain.cita.EstadoCita.NO_ASISTIO)
              AND (:idExcluido IS NULL OR c.id <> :idExcluido)
            """)
    boolean existeSolapamiento(@Param("medicoId") Long medicoId, @Param("fecha") LocalDate fecha,
                                @Param("hora") LocalTime hora, @Param("idExcluido") Long idExcluido);


    @Query(value = """
            SELECT * FROM cita c
            WHERE c.estado IN ('PROGRAMADA', 'CONFIRMADA')
              AND (c.fecha + c.hora) BETWEEN :desde AND :hasta
            """, nativeQuery = true)
    List<CitaJpaEntity> listarProximasEnVentana(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    @Query("""
            SELECT c, p.nombres, p.apellidos, p.documentoIdentidad, p.telefono, u.nombres, p.telegramChatId
            FROM CitaJpaEntity c
            JOIN PacienteJpaEntity p ON p.id = c.pacienteId
            LEFT JOIN UsuarioJpaEntity u ON u.id = c.medicoId
            WHERE c.fecha = :fecha
              AND c.origen <> com.threepartners.oncologia.domain.cita.OrigenCita.CAPTURA_PRETEST
            ORDER BY c.hora
            """)
    List<Object[]> agendaDelDia(@Param("fecha") LocalDate fecha);

    @Query("""
            SELECT c, p.nombres, p.apellidos, p.documentoIdentidad, p.telefono, u.nombres, p.telegramChatId
            FROM CitaJpaEntity c
            JOIN PacienteJpaEntity p ON p.id = c.pacienteId
            LEFT JOIN UsuarioJpaEntity u ON u.id = c.medicoId
            WHERE c.fecha < :antesDe
              AND c.estado IN (com.threepartners.oncologia.domain.cita.EstadoCita.PROGRAMADA,
                               com.threepartners.oncologia.domain.cita.EstadoCita.CONFIRMADA)
              AND c.origen <> com.threepartners.oncologia.domain.cita.OrigenCita.CAPTURA_PRETEST
            ORDER BY c.fecha, c.hora
            """)
    List<Object[]> pendientesDeCierre(@Param("antesDe") LocalDate antesDe);

    @Query(value = """
            SELECT * FROM cita c
            WHERE c.estado IN ('PROGRAMADA', 'CONFIRMADA')
              AND c.origen <> 'CAPTURA_PRETEST'
              AND (c.fecha + c.hora) < :limite
            """, nativeQuery = true)
    List<CitaJpaEntity> sinDesenlaceAntesDe(@Param("limite") LocalDateTime limite);

    long countByEstadoAndFechaBetween(EstadoCita estado, LocalDate desde, LocalDate hasta);

    long countByFechaBetween(LocalDate desde, LocalDate hasta);

    boolean existsByPacienteIdAndFechaAndHoraAndOrigen(Long pacienteId, LocalDate fecha, LocalTime hora,
                                                        com.threepartners.oncologia.domain.cita.OrigenCita origen);
}

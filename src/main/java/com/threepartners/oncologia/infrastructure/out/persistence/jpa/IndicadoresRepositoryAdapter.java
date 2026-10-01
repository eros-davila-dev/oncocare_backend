package com.threepartners.oncologia.infrastructure.out.persistence.jpa;

import com.threepartners.oncologia.domain.cita.EstadoCita;
import com.threepartners.oncologia.domain.dashboard.Indicadores;
import com.threepartners.oncologia.domain.dashboard.IndicadoresRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class IndicadoresRepositoryAdapter implements IndicadoresRepositoryPort {

    private final CitaJpaRepository citaJpaRepository;
    private final CicloTratamientoJpaRepository cicloTratamientoJpaRepository;
    private final PacienteJpaRepository pacienteJpaRepository;

    @Override
    public Indicadores calcular(LocalDate desde, LocalDate hasta) {
        long citasTotales = citaJpaRepository.countByFechaBetween(desde, hasta);
        long noAsistio = citaJpaRepository.countByEstadoAndFechaBetween(EstadoCita.NO_ASISTIO, desde, hasta);
        long atendidas = citaJpaRepository.countByEstadoAndFechaBetween(EstadoCita.ATENDIDA, desde, hasta);

        double tasaAusentismo = citasTotales == 0 ? 0.0 : (noAsistio * 100.0) / citasTotales;
        double cumplimiento = promedioCumplimientoSeguro();
        double tiempoPromedioRegistro = promedioTiempoRegistroSeguro();

        return new Indicadores(tiempoPromedioRegistro, tasaAusentismo, atendidas, citasTotales, cumplimiento);
    }

    private double promedioCumplimientoSeguro() {
        Double promedio = cicloTratamientoJpaRepository.promedioCumplimiento();
        return promedio != null ? promedio : 0.0;
    }

    private double promedioTiempoRegistroSeguro() {
        Double promedio = pacienteJpaRepository.promedioTiempoRegistroSegundos();
        return promedio != null ? promedio : 0.0;
    }
}

package com.threepartners.oncologia.application.cita;

import com.threepartners.oncologia.domain.cita.Cita;
import com.threepartners.oncologia.domain.cita.CitaRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Usado por el flujo de recordatorios de n8n (seccion 15, flujo 2): expone las
 * citas dentro de la ventana de recordatorio (24-48h antes) para que el
 * workflow envie el aviso por WhatsApp/correo.
 */
@Service
@RequiredArgsConstructor
public class ListarCitasProximasUseCase {

    private final CitaRepositoryPort citaRepositoryPort;

    @Transactional(readOnly = true)
    public List<Cita> ejecutar(int horasDesde, int horasHasta) {
        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime desde = ahora.plusHours(horasDesde);
        LocalDateTime hasta = ahora.plusHours(horasHasta);
        return citaRepositoryPort.listarProximasEnVentana(desde, hasta);
    }
}

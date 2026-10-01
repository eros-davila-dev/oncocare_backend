package com.threepartners.oncologia.application.dashboard;

import com.threepartners.oncologia.domain.dashboard.Indicadores;
import com.threepartners.oncologia.domain.dashboard.IndicadoresRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class ConsultarIndicadoresUseCase {

    private final IndicadoresRepositoryPort indicadoresRepositoryPort;

    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'RECEPCIONISTA')")
    @Transactional(readOnly = true)
    public Indicadores ejecutar(LocalDate desde, LocalDate hasta) {
        LocalDate hastaEfectivo = hasta != null ? hasta : LocalDate.now();
        LocalDate desdeEfectivo = desde != null ? desde : hastaEfectivo.minusDays(30);
        return indicadoresRepositoryPort.calcular(desdeEfectivo, hastaEfectivo);
    }
}

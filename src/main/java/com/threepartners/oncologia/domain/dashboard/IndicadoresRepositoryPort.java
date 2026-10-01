package com.threepartners.oncologia.domain.dashboard;

import java.time.LocalDate;

public interface IndicadoresRepositoryPort {

    Indicadores calcular(LocalDate desde, LocalDate hasta);
}

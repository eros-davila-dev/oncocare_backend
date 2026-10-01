package com.threepartners.oncologia.application.cita;

import com.threepartners.oncologia.config.AgendaProperties;
import com.threepartners.oncologia.domain.cita.Cita;
import com.threepartners.oncologia.domain.cita.CitaRepositoryPort;
import com.threepartners.oncologia.domain.cita.EstadoCita;
import com.threepartners.oncologia.domain.shared.ZonaHoraria;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CerrarCitasSinDesenlaceUseCaseTest {

    @Mock
    private CitaRepositoryPort citaRepositoryPort;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Test
    void cierraComoNoAsistioConMarcaAutomaticaLasCitasVencidasHace48Horas() {
        // 5/10/2026 10:00 en Lima
        Clock clock = Clock.fixed(Instant.parse("2026-10-05T15:00:00Z"), ZonaHoraria.LIMA);
        var useCase = new CerrarCitasSinDesenlaceUseCase(citaRepositoryPort, new AgendaProperties(48), eventPublisher, clock);
        Cita olvidada = Cita.builder().id(7L).fecha(LocalDate.of(2026, 10, 2)).hora(LocalTime.of(9, 0))
                .estado(EstadoCita.CONFIRMADA).build();
        when(citaRepositoryPort.sinDesenlaceAntesDe(LocalDateTime.of(2026, 10, 3, 10, 0))).thenReturn(List.of(olvidada));

        int cerradas = useCase.ejecutar();

        assertThat(cerradas).isEqualTo(1);
        assertThat(olvidada.getEstado()).isEqualTo(EstadoCita.NO_ASISTIO);
        assertThat(olvidada.isCierreAutomatico()).isTrue();
        assertThat(olvidada.getDesenlaceRegistradoPor()).isNull();
        verify(citaRepositoryPort).guardar(olvidada);
    }
}

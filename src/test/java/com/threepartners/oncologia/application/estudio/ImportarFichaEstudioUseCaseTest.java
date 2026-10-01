package com.threepartners.oncologia.application.estudio;

import com.threepartners.oncologia.application.estudio.FichasEstudio.TipoFicha;
import com.threepartners.oncologia.domain.cita.Cita;
import com.threepartners.oncologia.domain.cita.CitaRepositoryPort;
import com.threepartners.oncologia.domain.estudio.ConsultaRepositoryPort;
import com.threepartners.oncologia.domain.estudio.MedicionRegistroRepositoryPort;
import com.threepartners.oncologia.domain.estudio.ParticipanteEstudio;
import com.threepartners.oncologia.domain.estudio.ParticipanteEstudioRepositoryPort;
import com.threepartners.oncologia.domain.paciente.PacienteRepositoryPort;
import com.threepartners.oncologia.domain.shared.exception.ValidacionDeNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImportarFichaEstudioUseCaseTest {

    @Mock
    private PreparadorFichasEstudio preparador;
    @Mock
    private ParticipanteEstudioRepositoryPort participanteRepositoryPort;
    @Mock
    private PacienteRepositoryPort pacienteRepositoryPort;
    @Mock
    private MedicionRegistroRepositoryPort medicionRegistroRepositoryPort;
    @Mock
    private CitaRepositoryPort citaRepositoryPort;
    @Mock
    private ConsultaRepositoryPort consultaRepositoryPort;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private ImportarFichaEstudioUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ImportarFichaEstudioUseCase(preparador, participanteRepositoryPort, pacienteRepositoryPort,
                medicionRegistroRepositoryPort, citaRepositoryPort, consultaRepositoryPort, eventPublisher);
        lenient().when(participanteRepositoryPort.buscarPorCodigo("P01"))
                .thenReturn(Optional.of(ParticipanteEstudio.builder().codigo("P01").pacienteId(3L).incluido(true).build()));
        lenient().when(preparador.prepararAsistencia(any(), any())).thenAnswer(inv -> {
            FichasEstudio.FichaAsistencia f = inv.getArgument(0);
            return Cita.capturaPretest(f.pacienteId(), f.fecha(), f.hora(), f.tipoConsulta(), f.asistio(), 1L, Instant.EPOCH);
        });
    }

    private static Map<String, String> fila(String paciente, String fecha, String hora, String asistio) {
        Map<String, String> fila = new LinkedHashMap<>();
        fila.put("paciente", paciente);
        fila.put("fecha_de_cita", fecha);
        fila.put("hora", hora);
        fila.put("asistio", asistio);
        return fila;
    }

    @Test
    void aceptaLosEncabezadosYFormatosDeLaFichaDeLaTesis() {
        var filas = List.of(fila("P01", "15/09/2026", "9:30", "Sí"), fila("P01", "2026-09-22", "", "NO"));

        var resultado = useCase.ejecutar(TipoFicha.ASISTENCIAS, filas, true, 1L, "ip");

        assertThat(resultado.errores()).isEmpty();
        assertThat(resultado.aplicado()).isTrue();
        assertThat(resultado.filasValidas()).isEqualTo(2);
        verify(citaRepositoryPort, times(2)).guardar(any(Cita.class));
        verify(preparador).prepararAsistencia(eq(new FichasEstudio.FichaAsistencia(
                3L, LocalDate.of(2026, 9, 15), LocalTime.of(9, 30), null, true)), eq(1L));
    }

    @Test
    void siUnaFilaTieneErroresNoSeGuardaNinguna() {
        var filas = List.of(fila("P01", "15/09/2026", "09:30", "SI"), fila("P01", "fecha-mala", "09:30", "SI"));

        var resultado = useCase.ejecutar(TipoFicha.ASISTENCIAS, filas, true, 1L, "ip");

        assertThat(resultado.aplicado()).isFalse();
        assertThat(resultado.errores()).hasSize(1);
        assertThat(resultado.errores().getFirst().fila()).isEqualTo(3);
        verify(citaRepositoryPort, never()).guardar(any());
    }

    @Test
    void laValidacionPreviaNoGuardaNada() {
        var resultado = useCase.ejecutar(TipoFicha.ASISTENCIAS, List.of(fila("P01", "15/09/2026", "09:30", "SI")), false, 1L, "ip");

        assertThat(resultado.filasValidas()).isEqualTo(1);
        assertThat(resultado.aplicado()).isFalse();
        verify(citaRepositoryPort, never()).guardar(any());
    }

    @Test
    void detectaFilasDuplicadasDentroDelMismoArchivo() {
        var filas = List.of(fila("P01", "15/09/2026", "09:30", "SI"), fila("P01", "2026-09-15", "9:30", "NO"));

        var resultado = useCase.ejecutar(TipoFicha.ASISTENCIAS, filas, true, 1L, "ip");

        assertThat(resultado.errores()).extracting(FichasEstudio.ErrorFila::mensaje).containsExactly("Fila duplicada dentro del mismo archivo");
    }

    @Test
    void rechazaArchivosSinLasColumnasObligatorias() {
        Map<String, String> fila = Map.of("paciente", "P01", "fecha", "2026-09-15");

        assertThatThrownBy(() -> useCase.ejecutar(TipoFicha.ASISTENCIAS, List.of(fila), false, 1L, "ip"))
                .isInstanceOf(ValidacionDeNegocioException.class)
                .hasMessageContaining("asistio");
    }

    @Test
    void interpretaValoresSiNoYFechasEnVariosFormatos() {
        assertThat(ImportarFichaEstudioUseCase.siNo("sí", "x")).isTrue();
        assertThat(ImportarFichaEstudioUseCase.siNo("N", "x")).isFalse();
        assertThat(ImportarFichaEstudioUseCase.fecha("5/9/2026")).isEqualTo(LocalDate.of(2026, 9, 5));
        assertThat(ImportarFichaEstudioUseCase.hora("14:05")).isEqualTo(LocalTime.of(14, 5));
        assertThatThrownBy(() -> ImportarFichaEstudioUseCase.siNo("tal vez", "asistio"))
                .isInstanceOf(ValidacionDeNegocioException.class);
    }
}

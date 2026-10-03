package com.threepartners.oncologia.application.recordatorio;

import com.threepartners.oncologia.config.FrontendProperties;
import com.threepartners.oncologia.domain.cita.Cita;
import com.threepartners.oncologia.domain.cita.CitaRepositoryPort;
import com.threepartners.oncologia.domain.cita.EstadoCita;
import com.threepartners.oncologia.domain.notificacion.NotificadorCorreoPort;
import com.threepartners.oncologia.domain.notificacion.TipoCorreo;
import com.threepartners.oncologia.domain.paciente.Paciente;
import com.threepartners.oncologia.domain.paciente.PacienteRepositoryPort;
import com.threepartners.oncologia.domain.recordatorio.CanalRecordatorio;
import com.threepartners.oncologia.domain.recordatorio.EstadoRecordatorio;
import com.threepartners.oncologia.domain.recordatorio.Recordatorio;
import com.threepartners.oncologia.domain.recordatorio.RecordatorioRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnviarRecordatoriosCorreoUseCaseTest {

    /** 5/10/2026 10:00 en Lima. */
    private static final Instant AHORA = Instant.parse("2026-10-05T15:00:00Z");

    @Mock
    private RecordatorioRepositoryPort recordatorios;
    @Mock
    private CitaRepositoryPort citas;
    @Mock
    private PacienteRepositoryPort pacientes;
    @Mock
    private NotificadorCorreoPort correo;

    private EnviarRecordatoriosCorreoUseCase useCase;
    private Cita cita;
    private Recordatorio aviso;

    @BeforeEach
    void setUp() {
        useCase = new EnviarRecordatoriosCorreoUseCase(recordatorios, citas, pacientes, correo,
                new FrontendProperties("https://portal.fundacion.pe"), Clock.fixed(AHORA, ZoneOffset.UTC));
        // Cita el 6/10 a las 11:00 en Lima; el aviso es el de 24 h, con el momento que le corresponde.
        cita = Cita.builder().id(1L).pacienteId(2L).fecha(LocalDate.of(2026, 10, 6)).hora(LocalTime.of(11, 0))
                .estado(EstadoCita.PROGRAMADA).build();
        aviso = Recordatorio.planificar(cita, CanalRecordatorio.CORREO, AHORA.minusSeconds(7200)).getLast();
        aviso.setId(9L);
        when(recordatorios.bloquearPendientesVencidos(eq(CanalRecordatorio.CORREO), eq(AHORA), anyInt()))
                .thenReturn(List.of(aviso));
        when(citas.buscarPorId(1L)).thenReturn(Optional.of(cita));
    }

    private Paciente.PacienteBuilder paciente() {
        return Paciente.builder().id(2L).nombres("Ana Maria").email("ana@correo.pe")
                .contactoEmergenciaNombre("Lucia Perez").contactoEmergenciaEmail("lucia@correo.pe");
    }

    @Test
    void enviaAlPacienteYAlReferidoSiLoAutorizo() {
        when(pacientes.buscarPorId(2L)).thenReturn(Optional.of(paciente().usuarioId(5L).contactoRecibeRecordatorios(true).build()));

        assertThat(useCase.ejecutar()).isEqualTo(1);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> datos = ArgumentCaptor.forClass(Map.class);
        verify(correo).enviar(eq(TipoCorreo.RECORDATORIO_CITA), eq("ana@correo.pe"), eq("Ana Maria"),
                eq("https://portal.fundacion.pe/mis-citas"), datos.capture());
        assertThat(datos.getValue()).containsEntry("hora", "11:00").containsEntry("paciente", "Ana");
        verify(correo).enviar(eq(TipoCorreo.RECORDATORIO_CITA_REFERIDO), eq("lucia@correo.pe"), eq("Lucia Perez"),
                eq(""), anyMap());
        assertThat(aviso.getEstado()).isEqualTo(EstadoRecordatorio.ENVIADO);
    }

    @Test
    void sinConsentimientoElReferidoNoRecibeNada() {
        when(pacientes.buscarPorId(2L)).thenReturn(Optional.of(paciente().contactoRecibeRecordatorios(false).build()));

        useCase.ejecutar();

        verify(correo, never()).enviar(eq(TipoCorreo.RECORDATORIO_CITA_REFERIDO), anyString(), anyString(), anyString(), anyMap());
        // Sin cuenta en el portal no hay enlace a "Mis citas".
        verify(correo).enviar(eq(TipoCorreo.RECORDATORIO_CITA), eq("ana@correo.pe"), anyString(), eq(""), anyMap());
    }

    @Test
    void siLaCitaSeCanceloElAvisoSeCancelaSinEnviar() {
        cita.cancelar();
        when(pacientes.buscarPorId(2L)).thenReturn(Optional.of(paciente().contactoRecibeRecordatorios(true).build()));

        assertThat(useCase.ejecutar()).isZero();

        verify(correo, never()).enviar(any(), anyString(), anyString(), anyString(), anyMap());
        assertThat(aviso.getEstado()).isEqualTo(EstadoRecordatorio.CANCELADO);
    }

    @Test
    void siElPacienteApagoLosRecordatoriosNoSeEnvia() {
        when(pacientes.buscarPorId(2L)).thenReturn(Optional.of(paciente().aceptaRecordatorios(false).build()));

        assertThat(useCase.ejecutar()).isZero();

        assertThat(aviso.getEstado()).isEqualTo(EstadoRecordatorio.CANCELADO);
    }
}

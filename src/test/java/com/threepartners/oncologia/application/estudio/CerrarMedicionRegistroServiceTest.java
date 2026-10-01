package com.threepartners.oncologia.application.estudio;

import com.threepartners.oncologia.domain.estudio.CanalMedicion;
import com.threepartners.oncologia.domain.estudio.EstadoMedicion;
import com.threepartners.oncologia.domain.estudio.MedicionRegistro;
import com.threepartners.oncologia.domain.estudio.MedicionRegistroRepositoryPort;
import com.threepartners.oncologia.domain.estudio.TipoMedicion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CerrarMedicionRegistroServiceTest {

    private static final Instant INICIO = Instant.parse("2026-10-05T14:00:00Z");

    @Mock
    private MedicionRegistroRepositoryPort repositorio;

    private final Clock clock = Clock.fixed(INICIO.plusSeconds(420), ZoneOffset.UTC);
    private CerrarMedicionRegistroService servicio;

    @BeforeEach
    void setUp() {
        servicio = new CerrarMedicionRegistroService(repositorio, clock);
    }

    private MedicionRegistro abierta(Long usuarioId) {
        MedicionRegistro m = MedicionRegistro.iniciar(TipoMedicion.REGISTRO_CITA, CanalMedicion.INTRANET, usuarioId, INICIO);
        m.setId(5L);
        return m;
    }

    @Test
    void sellaElFinConElRelojDelServidor() {
        MedicionRegistro medicion = abierta(7L);
        when(repositorio.buscarPorId(5L)).thenReturn(Optional.of(medicion));

        servicio.cerrar(5L, TipoMedicion.REGISTRO_CITA, 7L, 3L, 90L);

        verify(repositorio).guardar(medicion);
        assertThat(medicion.getEstado()).isEqualTo(EstadoMedicion.COMPLETADA);
        assertThat(medicion.duracion().toMinutes()).isEqualTo(7);
    }

    @Test
    void sinSesionDeMedicionNoHaceNada() {
        servicio.cerrar(null, TipoMedicion.REGISTRO_CITA, 7L, 3L, 90L);

        verifyNoInteractions(repositorio);
    }

    @Test
    void unaSesionAjenaNoSeCuentaPeroNoImpideGuardarElRegistro() {
        when(repositorio.buscarPorId(5L)).thenReturn(Optional.of(abierta(7L)));

        assertThatCode(() -> servicio.cerrar(5L, TipoMedicion.REGISTRO_CITA, 8L, 3L, 90L)).doesNotThrowAnyException();
        verify(repositorio, never()).guardar(any());
    }

    @Test
    void unaSesionInexistenteNoImpideGuardarElRegistro() {
        when(repositorio.buscarPorId(5L)).thenReturn(Optional.empty());

        assertThatCode(() -> servicio.cerrar(5L, TipoMedicion.REGISTRO_CITA, 7L, 3L, 90L)).doesNotThrowAnyException();
    }
}

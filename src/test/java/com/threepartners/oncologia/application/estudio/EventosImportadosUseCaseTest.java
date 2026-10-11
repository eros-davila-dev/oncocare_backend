package com.threepartners.oncologia.application.estudio;

import com.threepartners.oncologia.domain.estudio.EventoImportado;
import com.threepartners.oncologia.domain.estudio.EventosImportadosRepositoryPort;
import com.threepartners.oncologia.domain.estudio.Fase;
import com.threepartners.oncologia.domain.estudio.TipoEventoImportado;
import com.threepartners.oncologia.domain.shared.exception.ValidacionDeNegocioException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class EventosImportadosUseCaseTest {

    private final EventosImportadosRepositoryPort repositorio = mock(EventosImportadosRepositoryPort.class);
    private final EventosImportadosUseCase useCase = new EventosImportadosUseCase(repositorio, mock(ApplicationEventPublisher.class));

    @Test
    @SuppressWarnings("unchecked")
    void leeLasTresFichasConCodigoAnonimoYSuFuente() {
        var resultado = useCase.cargar(Map.of(
                "TPR_PRE", List.of(
                        Map.of("id_registro", "Ej.", "fecha", "2026-08-03", "codigo_paciente", "PAC-00"),
                        Map.of("id_registro", "1", "fecha", "2026-08-03", "codigo_paciente", "PAC-01",
                                "hora_inicio_registro", "09:02", "hora_fin_registro", "09:09", "tpr", "7.4999999",
                                "modalidad", "Manual", "control_de_calidad", "OK")),
                "Ausentismo_POST", List.of(Map.of("id_cita", "1", "fecha", "2026-09-02", "codigo_paciente", "PAC-234",
                        "estado", "No asistió", "motivo", "Olvido", "recordatorio_enviado", "Sí")),
                "Consultas_POST", List.of(Map.of("id_consulta", "1", "fecha", "2026-09-02", "codigo_paciente", "PAC-234",
                        "categoria", "Citas", "canal", "Chatbot", "resuelta_en_el_primer_contacto", "Sí",
                        "derivada_a_otra_persona_o_area", "No", "tiempo_de_respuesta", "1.50"))), 7L, "ip");

        assertThat(resultado.total()).isEqualTo(3);
        ArgumentCaptor<List<EventoImportado>> guardados = ArgumentCaptor.forClass(List.class);
        verify(repositorio).reemplazar(guardados.capture(), eq(7L));
        var eventos = guardados.getValue();

        var registro = eventos.stream().filter(e -> e.tipo() == TipoEventoImportado.REGISTRO).findFirst().orElseThrow();
        assertThat(registro.fase()).isEqualTo(Fase.PRETEST);
        assertThat(registro.codigoPaciente()).isEqualTo("PAC-01");
        assertThat(registro.minutos()).isEqualTo(7.5);
        assertThat(registro.horaInicio()).isEqualTo(LocalTime.of(9, 2));
        assertThat(registro.fuente()).contains("ficha aplicada a mano");

        var cita = eventos.stream().filter(e -> e.tipo() == TipoEventoImportado.CITA).findFirst().orElseThrow();
        assertThat(cita.fase()).isEqualTo(Fase.POSTEST);
        assertThat(cita.recordatorio()).isTrue();
        assertThat(cita.fuente()).contains("servidor de la fundación");

        var consulta = eventos.stream().filter(e -> e.tipo() == TipoEventoImportado.CONSULTA).findFirst().orElseThrow();
        assertThat(consulta.resuelta()).isTrue();
        assertThat(consulta.derivada()).isFalse();
        assertThat(consulta.tiempoRespuesta()).isEqualTo(1.5);
    }

    @Test
    void sinHojasDelInstrumentoNoCargaNada() {
        assertThatThrownBy(() -> useCase.cargar(Map.of("Otra", List.of()), 7L, "ip"))
                .isInstanceOf(ValidacionDeNegocioException.class);
    }
}

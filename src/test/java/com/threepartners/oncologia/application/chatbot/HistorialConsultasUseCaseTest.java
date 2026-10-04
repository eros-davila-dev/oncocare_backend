package com.threepartners.oncologia.application.chatbot;

import com.threepartners.oncologia.domain.chatbot.ConversacionChatbot;
import com.threepartners.oncologia.domain.chatbot.ConversacionChatbotRepositoryPort;
import com.threepartners.oncologia.domain.estudio.CanalConsulta;
import com.threepartners.oncologia.domain.estudio.Consulta;
import com.threepartners.oncologia.domain.estudio.ConsultaRepositoryPort;
import com.threepartners.oncologia.domain.paciente.Paciente;
import com.threepartners.oncologia.domain.paciente.PacienteRepositoryPort;
import com.threepartners.oncologia.domain.shared.CriterioPaginacion;
import com.threepartners.oncologia.domain.shared.exception.RecursoNoEncontradoException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HistorialConsultasUseCaseTest {

    private final ConsultaRepositoryPort consultas = mock(ConsultaRepositoryPort.class);
    private final ConversacionChatbotRepositoryPort conversaciones = mock(ConversacionChatbotRepositoryPort.class);
    private final PacienteRepositoryPort pacientes = mock(PacienteRepositoryPort.class);
    private final HistorialConsultasUseCase useCase = new HistorialConsultasUseCase(consultas, conversaciones, pacientes);

    @Test
    void elPacienteVeLaConversacionDeSuConsulta() {
        when(pacientes.buscarPorUsuarioId(10L)).thenReturn(Optional.of(paciente(1L)));
        when(consultas.buscarPorId(5L)).thenReturn(Optional.of(consultaDe(5L, 1L)));
        var turno = ConversacionChatbot.builder().consultaId(5L).mensajeUsuario("Hola").respuestaBot("Hola").build();
        when(conversaciones.listarPorConsulta(5L)).thenReturn(List.of(turno));

        assertThat(useCase.miConversacion(10L, 5L)).containsExactly(turno);
    }

    @Test
    void laConsultaDeOtroPacienteNoExisteParaEl() {
        when(pacientes.buscarPorUsuarioId(10L)).thenReturn(Optional.of(paciente(1L)));
        when(consultas.buscarPorId(6L)).thenReturn(Optional.of(consultaDe(6L, 2L)));

        assertThatThrownBy(() -> useCase.miConversacion(10L, 6L)).isInstanceOf(RecursoNoEncontradoException.class);
        verify(conversaciones, never()).listarPorConsulta(any());
    }

    @Test
    void unaConsultaAnonimaTampocoEsDeNadie() {
        when(pacientes.buscarPorUsuarioId(10L)).thenReturn(Optional.of(paciente(1L)));
        when(consultas.buscarPorId(7L)).thenReturn(Optional.of(consultaDe(7L, null)));

        assertThatThrownBy(() -> useCase.miConversacion(10L, 7L)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void sinFichaDePacienteNoHayConsultas() {
        when(pacientes.buscarPorUsuarioId(10L)).thenReturn(Optional.empty());

        assertThat(useCase.misConsultas(10L, CriterioPaginacion.de(0, 20)).contenido()).isEmpty();
        verify(consultas, never()).listarPorPaciente(any(), any());
    }

    private static Paciente paciente(Long id) {
        return Paciente.builder().id(id).nombres("Ana").apellidos("Perez").documentoIdentidad("12345678")
                .telefono("904049494").build();
    }

    private static Consulta consultaDe(Long id, Long pacienteId) {
        Consulta c = Consulta.abrir(CanalConsulta.CHATBOT_WEB, "s-" + id, pacienteId, "GENERAL_QUERY", "Horarios",
                Instant.now(), Instant.now());
        c.setId(id);
        return c;
    }
}

package com.threepartners.oncologia.application.chatbot;

import com.threepartners.oncologia.domain.estudio.CanalConsulta;
import com.threepartners.oncologia.domain.estudio.Consulta;
import com.threepartners.oncologia.domain.estudio.ConsultaRepositoryPort;
import com.threepartners.oncologia.domain.estudio.ResultadoConsulta;
import com.threepartners.oncologia.domain.notificacion.NotificadorExternoPort;
import com.threepartners.oncologia.domain.paciente.Paciente;
import com.threepartners.oncologia.domain.paciente.PacienteRepositoryPort;
import com.threepartners.oncologia.domain.shared.exception.RecursoNoEncontradoException;
import com.threepartners.oncologia.domain.usuario.Rol;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Map;

/**
 * Acciones del usuario del chat sobre su propia consulta: valorar la
 * respuesta (👍/👎) y pedir hablar con una persona. El chat es publico, asi
 * que la propiedad se verifica por el identificador de sesion: solo quien
 * tiene la sesion de esa conversacion puede valorarla.
 */
@Service
@RequiredArgsConstructor
public class InteractuarConsultaChatbotUseCase {

    private final ConsultaRepositoryPort consultaRepositoryPort;
    private final PacienteRepositoryPort pacienteRepositoryPort;
    private final GestorConsultasChatbot gestorConsultas;
    private final NotificadorExternoPort notificadorExternoPort;
    private final Clock clock;

    @Transactional
    public Consulta valorar(Long consultaId, int valor, String sesionId) {
        Consulta consulta = consultaRepositoryPort.buscarPorId(consultaId)
                .filter(c -> c.getSesionId() != null && c.getSesionId().equals(sesionId))
                .orElseThrow(() -> new RecursoNoEncontradoException("Consulta", consultaId));
        boolean estabaResuelta = consulta.getResultado() == ResultadoConsulta.RESUELTA_BOT;
        consulta.valorar(valor, clock.instant());
        Consulta guardada = consultaRepositoryPort.guardar(consulta);
        if (estabaResuelta && guardada.getResultado() == ResultadoConsulta.ESCALADA) {
            notificadorExternoPort.dispararWorkflow("/webhook/consultas/escalada",
                    Map.of("consultaId", guardada.getId(), "canal", guardada.getCanal().name()));
        }
        return guardada;
    }

    @Transactional
    public Consulta hablarConUnaPersona(String sesionId, CanalConsulta canal, Long usuarioId, Rol rol) {
        Long pacienteId = rol == Rol.PACIENTE && usuarioId != null
                ? pacienteRepositoryPort.buscarPorUsuarioId(usuarioId).map(Paciente::getId).orElse(null)
                : null;
        return gestorConsultas.escalarPorPedidoDelUsuario(sesionId, canal != null ? canal : CanalConsulta.CHATBOT_WEB, pacienteId);
    }
}

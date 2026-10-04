package com.threepartners.oncologia.application.recordatorio;

import com.threepartners.oncologia.domain.cita.Cita;
import com.threepartners.oncologia.domain.cita.CitaRepositoryPort;
import com.threepartners.oncologia.domain.paciente.Paciente;
import com.threepartners.oncologia.domain.paciente.PacienteRepositoryPort;
import com.threepartners.oncologia.domain.recordatorio.CanalRecordatorio;
import com.threepartners.oncologia.domain.recordatorio.Recordatorio;
import com.threepartners.oncologia.domain.recordatorio.RecordatorioRepositoryPort;
import com.threepartners.oncologia.domain.shared.exception.RecursoNoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Contrato con n8n: entrega los recordatorios de Telegram (al paciente y a
 * su referido, cada uno con su texto) que ya deben
 * enviarse (con el texto listo) y recibe el resultado de cada envio. Antes de
 * entregar cada aviso verifica que siga vigente: si la cita se cancelo o se
 * reprogramo despues de programarlo, el aviso se cancela en vez de enviarse.
 */
@Service
@RequiredArgsConstructor
public class EntregarRecordatoriosUseCase {

    static final int LIMITE_MAXIMO = 100;

    private final RecordatorioRepositoryPort recordatorioRepositoryPort;
    private final CitaRepositoryPort citaRepositoryPort;
    private final PacienteRepositoryPort pacienteRepositoryPort;
    private final Clock clock;

    @Transactional
    public List<RecordatorioParaEnviar> tomarPendientes(int limite) {
        Instant ahora = clock.instant();
        List<RecordatorioParaEnviar> paraEnviar = new ArrayList<>();
        int tope = Math.clamp(limite, 1, LIMITE_MAXIMO);
        List<Recordatorio> vencidos = new ArrayList<>(
                recordatorioRepositoryPort.bloquearPendientesVencidos(CanalRecordatorio.TELEGRAM, ahora, tope));
        if (vencidos.size() < tope) {
            vencidos.addAll(recordatorioRepositoryPort.bloquearPendientesVencidos(
                    CanalRecordatorio.TELEGRAM_REFERIDO, ahora, tope - vencidos.size()));
        }
        for (Recordatorio recordatorio : vencidos) {
            boolean alReferido = recordatorio.getCanal() == CanalRecordatorio.TELEGRAM_REFERIDO;
            Cita cita = citaRepositoryPort.buscarPorId(recordatorio.getCitaId()).orElse(null);
            Paciente paciente = cita != null ? pacienteRepositoryPort.buscarPorId(cita.getPacienteId()).orElse(null) : null;
            boolean destinatarioVigente = paciente != null
                    && (alReferido ? paciente.referidoRecibeTelegram() : paciente.tieneTelegram());
            if (cita == null || !destinatarioVigente || !recordatorio.vigentePara(cita, ahora)) {
                recordatorio.cancelar();
                recordatorioRepositoryPort.guardar(recordatorio);
                continue;
            }
            recordatorio.tomar(ahora);
            recordatorioRepositoryPort.guardar(recordatorio);
            paraEnviar.add(new RecordatorioParaEnviar(recordatorio.getId(),
                    alReferido ? paciente.getContactoTelegramChatId() : paciente.getTelegramChatId(),
                    alReferido ? MensajesRecordatorio.textoReferido(recordatorio.getTipo(), paciente, cita)
                            : MensajesRecordatorio.texto(recordatorio.getTipo(), paciente, cita),
                    MensajesRecordatorio.fechaTexto(cita), MensajesRecordatorio.horaTexto(cita), recordatorio.getTipo().name()));
        }
        return paraEnviar;
    }

    @Transactional
    public void registrarResultado(Long recordatorioId, boolean enviado, String mensajeExternoId, String error) {
        Recordatorio recordatorio = recordatorioRepositoryPort.buscarPorId(recordatorioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Recordatorio", recordatorioId));
        if (enviado) {
            recordatorio.marcarEnviado(mensajeExternoId, clock.instant());
        } else {
            recordatorio.marcarFallido(error != null ? error : "Error no informado por n8n");
        }
        recordatorioRepositoryPort.guardar(recordatorio);
    }

    /**
     * Lo que n8n necesita para enviar el mensaje con los botones: el texto ya
     * armado y el id para el callback_data ("r:<id>:<accion>", max 64 bytes).
     */
    public record RecordatorioParaEnviar(Long recordatorioId, Long chatId, String texto, String fechaTexto,
                                         String horaTexto, String tipo) {
    }
}

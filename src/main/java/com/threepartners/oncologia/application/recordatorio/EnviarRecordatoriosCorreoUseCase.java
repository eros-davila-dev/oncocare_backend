package com.threepartners.oncologia.application.recordatorio;

import com.threepartners.oncologia.config.FrontendProperties;
import com.threepartners.oncologia.domain.cita.Cita;
import com.threepartners.oncologia.domain.cita.CitaRepositoryPort;
import com.threepartners.oncologia.domain.notificacion.NotificadorCorreoPort;
import com.threepartners.oncologia.domain.notificacion.TipoCorreo;
import com.threepartners.oncologia.domain.paciente.Paciente;
import com.threepartners.oncologia.domain.paciente.PacienteRepositoryPort;
import com.threepartners.oncologia.domain.recordatorio.CanalRecordatorio;
import com.threepartners.oncologia.domain.recordatorio.Recordatorio;
import com.threepartners.oncologia.domain.recordatorio.RecordatorioRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;

/**
 * Envia los recordatorios por correo cuyo momento ya llego: al paciente y,
 * si lo autorizo, una copia a su referido. A diferencia de Telegram no pasa
 * por n8n: el correo se encola en el outbox en esta misma transaccion, y el
 * relevo lo entrega con reintentos (un proveedor caido no pierde el aviso).
 *
 * Como en Telegram, antes de enviar se verifica que el aviso siga vigente:
 * si la cita se cancelo o se reprogramo, se cancela en vez de enviarse.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnviarRecordatoriosCorreoUseCase {

    static final int LIMITE_POR_EJECUCION = 50;

    private final RecordatorioRepositoryPort recordatorioRepositoryPort;
    private final CitaRepositoryPort citaRepositoryPort;
    private final PacienteRepositoryPort pacienteRepositoryPort;
    private final NotificadorCorreoPort notificadorCorreo;
    private final FrontendProperties frontendProperties;
    private final Clock clock;

    @Transactional
    public int ejecutar() {
        Instant ahora = clock.instant();
        int enviados = 0;
        for (Recordatorio recordatorio : recordatorioRepositoryPort.bloquearPendientesVencidos(
                CanalRecordatorio.CORREO, ahora, LIMITE_POR_EJECUCION)) {
            Cita cita = citaRepositoryPort.buscarPorId(recordatorio.getCitaId()).orElse(null);
            Paciente paciente = cita != null ? pacienteRepositoryPort.buscarPorId(cita.getPacienteId()).orElse(null) : null;
            if (cita == null || paciente == null || !paciente.tieneEmail() || !paciente.aceptaRecordatorios()
                    || !recordatorio.vigentePara(cita, ahora)) {
                recordatorio.cancelar();
                recordatorioRepositoryPort.guardar(recordatorio);
                continue;
            }

            Map<String, String> datos = Map.of(
                    "fecha", MensajesRecordatorio.fechaTexto(cita),
                    "hora", MensajesRecordatorio.horaTexto(cita),
                    "paciente", paciente.nombrePila());
            // Solo quien tiene cuenta en el portal puede gestionar la cita desde "Mis citas".
            String enlace = paciente.getUsuarioId() != null ? frontendProperties.baseUrl() + "/mis-citas" : "";
            notificadorCorreo.enviar(TipoCorreo.RECORDATORIO_CITA, paciente.getEmail(), paciente.getNombres(), enlace, datos);
            if (paciente.referidoRecibeRecordatorios()) {
                notificadorCorreo.enviar(TipoCorreo.RECORDATORIO_CITA_REFERIDO, paciente.getContactoEmergenciaEmail(),
                        paciente.getContactoEmergenciaNombre(), "", datos);
            }

            recordatorio.tomar(ahora);
            recordatorio.marcarEnviado(null, ahora);
            recordatorioRepositoryPort.guardar(recordatorio);
            enviados++;
        }
        if (enviados > 0) {
            log.info("{} recordatorios de cita encolados por correo", enviados);
        }
        return enviados;
    }
}

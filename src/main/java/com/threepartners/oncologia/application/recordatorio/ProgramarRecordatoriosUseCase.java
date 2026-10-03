package com.threepartners.oncologia.application.recordatorio;

import com.threepartners.oncologia.config.RecordatoriosProperties;
import com.threepartners.oncologia.domain.cita.Cita;
import com.threepartners.oncologia.domain.cita.CitaRepositoryPort;
import com.threepartners.oncologia.domain.paciente.Paciente;
import com.threepartners.oncologia.domain.paciente.PacienteRepositoryPort;
import com.threepartners.oncologia.domain.recordatorio.CanalRecordatorio;
import com.threepartners.oncologia.domain.recordatorio.Recordatorio;
import com.threepartners.oncologia.domain.recordatorio.RecordatorioRepositoryPort;
import com.threepartners.oncologia.domain.shared.ZonaHoraria;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Invocado por un job cada 15 minutos. Programa los recordatorios de las
 * citas de los proximos dias segun el canal de cada paciente (Telegram si lo
 * vinculo; si no, una llamada de recepcion) y devuelve a la cola los avisos
 * que n8n tomo pero nunca confirmo.
 *
 * Es idempotente: la combinacion cita + tipo + canal es unica, asi que
 * ejecutarlo de nuevo nunca duplica avisos.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProgramarRecordatoriosUseCase {

    static final Duration HORIZONTE = Duration.ofDays(4);
    static final Duration ESPERA_MAXIMA_N8N = Duration.ofMinutes(15);

    private final CitaRepositoryPort citaRepositoryPort;
    private final PacienteRepositoryPort pacienteRepositoryPort;
    private final RecordatorioRepositoryPort recordatorioRepositoryPort;
    private final RecordatoriosProperties recordatoriosProperties;
    private final Clock clock;

    @Transactional
    public int ejecutar() {
        Instant ahora = clock.instant();
        LocalDateTime desde = LocalDateTime.ofInstant(ahora, ZonaHoraria.LIMA);
        int creados = 0;

        for (Cita cita : citaRepositoryPort.listarProximasEnVentana(desde, desde.plus(HORIZONTE))) {
            Paciente paciente = pacienteRepositoryPort.buscarPorId(cita.getPacienteId()).orElse(null);
            if (paciente == null || !paciente.aceptaRecordatorios()) {
                continue;
            }
            CanalRecordatorio canal = paciente.tieneTelegram() ? CanalRecordatorio.TELEGRAM : CanalRecordatorio.LLAMADA;
            creados += programar(cita, canal, ahora);
            // El correo se suma al canal principal (no lo reemplaza): llega tambien al referido.
            if (recordatoriosProperties.correoHabilitado() && paciente.tieneEmail()) {
                creados += programar(cita, CanalRecordatorio.CORREO, ahora);
            }
        }

        for (Recordatorio colgado : recordatorioRepositoryPort.enProcesoTomadosAntesDe(ahora.minus(ESPERA_MAXIMA_N8N))) {
            colgado.liberar();
            recordatorioRepositoryPort.guardar(colgado);
        }
        if (creados > 0) {
            log.info("{} recordatorios de cita programados", creados);
        }
        return creados;
    }

    private int programar(Cita cita, CanalRecordatorio canal, Instant ahora) {
        int creados = 0;
        for (Recordatorio recordatorio : Recordatorio.planificar(cita, canal, ahora)) {
            if (!recordatorioRepositoryPort.existe(cita.getId(), recordatorio.getTipo(), canal)) {
                recordatorioRepositoryPort.guardar(recordatorio);
                creados++;
            }
        }
        return creados;
    }
}

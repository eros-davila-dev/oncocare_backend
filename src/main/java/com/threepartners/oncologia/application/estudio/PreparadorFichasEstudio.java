package com.threepartners.oncologia.application.estudio;

import com.threepartners.oncologia.application.estudio.FichasEstudio.FichaAsistencia;
import com.threepartners.oncologia.application.estudio.FichasEstudio.FichaConsulta;
import com.threepartners.oncologia.application.estudio.FichasEstudio.FichaTiempo;
import com.threepartners.oncologia.domain.cita.Cita;
import com.threepartners.oncologia.domain.cita.CitaRepositoryPort;
import com.threepartners.oncologia.domain.estudio.Consulta;
import com.threepartners.oncologia.domain.estudio.Fase;
import com.threepartners.oncologia.domain.estudio.FaseEstudio;
import com.threepartners.oncologia.domain.estudio.MedicionRegistro;
import com.threepartners.oncologia.domain.estudio.MedicionRegistroRepositoryPort;
import com.threepartners.oncologia.domain.estudio.ParticipanteEstudioRepositoryPort;
import com.threepartners.oncologia.domain.estudio.TipoMedicion;
import com.threepartners.oncologia.domain.paciente.PacienteRepositoryPort;
import com.threepartners.oncologia.domain.shared.ZonaHoraria;
import com.threepartners.oncologia.domain.shared.exception.ConflictoDeNegocioException;
import com.threepartners.oncologia.domain.shared.exception.ValidacionDeNegocioException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Valida una ficha y la convierte en el objeto de dominio que alimenta el
 * indicador, sin persistirla. Lo comparten la captura por formulario y la
 * importacion de hojas de calculo, de modo que ambas aplican exactamente las
 * mismas reglas.
 *
 * Reglas:
 * - Tiempos y asistencias solo existen como ficha en el PRETEST (en el
 *   postest los produce el propio sistema) y su paciente debe ser
 *   participante incluido del estudio.
 * - Las consultas por WhatsApp, llamada o en persona se registran en cualquier
 *   fase abierta: tambien ocurren durante el postest y deben contar en el NCA.
 */
@Service
@RequiredArgsConstructor
public class PreparadorFichasEstudio {

    private final PeriodosEstudioService periodosEstudioService;
    private final ParticipanteEstudioRepositoryPort participanteRepositoryPort;
    private final PacienteRepositoryPort pacienteRepositoryPort;
    private final MedicionRegistroRepositoryPort medicionRegistroRepositoryPort;
    private final CitaRepositoryPort citaRepositoryPort;
    private final Clock clock;

    public MedicionRegistro prepararTiempo(FichaTiempo ficha, Long investigadorId) {
        validarFecha(ficha.fecha());
        exigirFase(ficha.fecha(), Fase.PRETEST, "La ficha de registro de tiempos");
        exigirParticipanteIncluido(ficha.pacienteId());
        if (ficha.horaInicio() == null || ficha.horaFin() == null) {
            throw new ValidacionDeNegocioException("La hora de inicio y la hora de fin son obligatorias");
        }
        TipoMedicion tipo = ficha.tipo() != null ? ficha.tipo() : TipoMedicion.REGISTRO_CITA;
        Instant inicio = instante(ficha.fecha(), ficha.horaInicio());
        Instant fin = instante(ficha.fecha(), ficha.horaFin());
        if (medicionRegistroRepositoryPort.existeManual(ficha.pacienteId(), tipo, inicio)) {
            throw new ConflictoDeNegocioException("Ya existe una medicion de ese paciente con la misma fecha y hora de inicio");
        }
        return MedicionRegistro.manual(tipo, ficha.pacienteId(), inicio, fin, investigadorId, ficha.observacion());
    }

    public Cita prepararAsistencia(FichaAsistencia ficha, Long investigadorId) {
        validarFecha(ficha.fecha());
        exigirFase(ficha.fecha(), Fase.PRETEST, "La ficha de registro de ausentismo");
        exigirParticipanteIncluido(ficha.pacienteId());
        if (citaRepositoryPort.existeCapturaPretest(ficha.pacienteId(), ficha.fecha(), ficha.hora())) {
            throw new ConflictoDeNegocioException("Esa cita del pretest ya fue registrada para el paciente");
        }
        return Cita.capturaPretest(ficha.pacienteId(), ficha.fecha(), ficha.hora(), ficha.tipoConsulta(),
                ficha.asistio(), investigadorId, clock.instant());
    }

    public Consulta prepararConsulta(FichaConsulta ficha, Long investigadorId) {
        validarFecha(ficha.fecha());
        periodosEstudioService.exigirFaseAbiertaQueContiene(ficha.fecha());
        if (ficha.pacienteId() != null) {
            pacienteRepositoryPort.buscarPorId(ficha.pacienteId())
                    .orElseThrow(() -> new ValidacionDeNegocioException("El paciente indicado no existe"));
        }
        if (ficha.resumen() == null || ficha.resumen().isBlank()) {
            throw new ValidacionDeNegocioException("Describa brevemente la consulta recibida");
        }
        LocalTime hora = ficha.hora() != null ? ficha.hora() : LocalTime.NOON;
        return Consulta.manual(ficha.canal(), ficha.pacienteId(), recortar(ficha.resumen(), 300), ficha.resuelta(),
                instante(ficha.fecha(), hora), investigadorId, ficha.observacion());
    }

    private void validarFecha(LocalDate fecha) {
        if (fecha == null) {
            throw new ValidacionDeNegocioException("La fecha es obligatoria");
        }
        if (fecha.isAfter(LocalDate.now(clock))) {
            throw new ValidacionDeNegocioException("La fecha no puede ser futura");
        }
    }

    private void exigirFase(LocalDate fecha, Fase esperada, String ficha) {
        FaseEstudio fase = periodosEstudioService.exigirFaseAbiertaQueContiene(fecha);
        if (fase.getFase() != esperada) {
            throw new ValidacionDeNegocioException(
                    "%s solo se usa en el %s; en el %s el sistema registra este dato automaticamente"
                            .formatted(ficha, esperada, fase.getFase()));
        }
    }

    private void exigirParticipanteIncluido(Long pacienteId) {
        if (pacienteId == null) {
            throw new ValidacionDeNegocioException("El paciente es obligatorio");
        }
        var participante = participanteRepositoryPort.buscarPorPacienteId(pacienteId)
                .orElseThrow(() -> new ValidacionDeNegocioException(
                        "El paciente no es participante del estudio: incluyalo en la muestra primero"));
        if (!participante.isIncluido()) {
            throw new ValidacionDeNegocioException(
                    "El participante " + participante.getCodigo() + " esta excluido del estudio");
        }
    }

    private static Instant instante(LocalDate fecha, LocalTime hora) {
        return fecha.atTime(hora).atZone(ZonaHoraria.LIMA).toInstant();
    }

    private static String recortar(String texto, int maximo) {
        String limpio = texto.strip();
        return limpio.length() <= maximo ? limpio : limpio.substring(0, maximo);
    }
}

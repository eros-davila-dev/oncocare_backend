package com.threepartners.oncologia.domain.recordatorio;

import com.threepartners.oncologia.domain.cita.Cita;
import com.threepartners.oncologia.domain.cita.EstadoCita;
import com.threepartners.oncologia.domain.shared.ZonaHoraria;
import com.threepartners.oncologia.domain.shared.exception.ValidacionDeNegocioException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Un aviso de cita programado. El backend decide que recordar y cuando; n8n
 * solo lo envia y reporta el resultado, de modo que la regla se prueba con
 * JUnit y un reenvio nunca duplica avisos (UNIQUE cita + tipo + canal).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Recordatorio {

    /** Intentos de envio antes de darlo por fallido (Telegram puede fallar de forma transitoria). */
    public static final int MAXIMO_INTENTOS = 3;

    private Long id;
    private Long citaId;
    private TipoRecordatorio tipo;
    private CanalRecordatorio canal;
    private Instant programadoPara;
    private EstadoRecordatorio estado;
    private int intentos;
    private String mensajeExternoId;
    private Instant tomadoEn;
    private Instant enviadoEn;
    private RespuestaRecordatorio respuesta;
    private Instant respondidoEn;
    private String error;

    /**
     * Recordatorios que corresponden a una cita segun el canal disponible.
     * Solo se programan los momentos que aun no pasaron. Por llamada basta un
     * aviso el dia anterior: tres llamadas serian una carga para recepcion.
     * Por correo, 72 h y 24 h: un correo 2 h antes rara vez se lee a tiempo.
     */
    public static List<Recordatorio> planificar(Cita cita, CanalRecordatorio canal, Instant ahora) {
        List<Recordatorio> plan = new ArrayList<>();
        Instant momentoCita = momentoDe(cita);
        List<TipoRecordatorio> tipos = switch (canal) {
            case TELEGRAM -> List.of(TipoRecordatorio.values());
            case CORREO -> List.of(TipoRecordatorio.T72H, TipoRecordatorio.T24H);
            case LLAMADA -> List.of(TipoRecordatorio.T24H);
        };
        for (TipoRecordatorio tipo : tipos) {
            Instant programado = momentoCita.minus(tipo.anticipacion());
            if (programado.isAfter(ahora)) {
                plan.add(Recordatorio.builder()
                        .citaId(cita.getId())
                        .tipo(tipo)
                        .canal(canal)
                        .programadoPara(programado)
                        .estado(EstadoRecordatorio.PENDIENTE)
                        .build());
            }
        }
        return plan;
    }

    public static Instant momentoDe(Cita cita) {
        return cita.fechaHora().atZone(ZonaHoraria.LIMA).toInstant();
    }

    /**
     * Sigue teniendo sentido enviarlo: la cita sigue activa, no se reprogramo
     * (su hora coincide con la que origino este aviso) y aun no paso.
     */
    public boolean vigentePara(Cita cita, Instant ahora) {
        boolean citaActiva = cita.getEstado() == EstadoCita.PROGRAMADA || cita.getEstado() == EstadoCita.CONFIRMADA;
        Instant momentoCita = momentoDe(cita);
        return citaActiva
                && momentoCita.minus(tipo.anticipacion()).equals(programadoPara)
                && momentoCita.isAfter(ahora);
    }

    public void tomar(Instant ahora) {
        if (estado != EstadoRecordatorio.PENDIENTE) {
            throw new ValidacionDeNegocioException("El recordatorio " + id + " no esta pendiente");
        }
        this.estado = EstadoRecordatorio.EN_PROCESO;
        this.tomadoEn = ahora;
        this.intentos++;
    }

    public void marcarEnviado(String mensajeExternoId, Instant ahora) {
        this.estado = EstadoRecordatorio.ENVIADO;
        this.mensajeExternoId = mensajeExternoId;
        this.enviadoEn = ahora;
        this.error = null;
    }

    /** Un fallo transitorio vuelve a la cola hasta agotar los intentos. */
    public void marcarFallido(String error) {
        this.error = error;
        this.estado = intentos < MAXIMO_INTENTOS ? EstadoRecordatorio.PENDIENTE : EstadoRecordatorio.FALLIDO;
    }

    /** n8n tomo el aviso pero nunca informo el resultado (se cayo): se reintenta. */
    public void liberar() {
        if (estado == EstadoRecordatorio.EN_PROCESO) {
            this.estado = intentos < MAXIMO_INTENTOS ? EstadoRecordatorio.PENDIENTE : EstadoRecordatorio.FALLIDO;
        }
    }

    public void cancelar() {
        if (estado == EstadoRecordatorio.PENDIENTE || estado == EstadoRecordatorio.EN_PROCESO) {
            this.estado = EstadoRecordatorio.CANCELADO;
        }
    }

    public void registrarRespuesta(RespuestaRecordatorio respuesta, Instant ahora) {
        this.respuesta = respuesta;
        this.respondidoEn = ahora;
    }
}

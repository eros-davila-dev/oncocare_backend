package com.threepartners.oncologia.domain.estudio;

import com.threepartners.oncologia.domain.shared.exception.ValidacionDeNegocioException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Duration;
import java.time.Instant;

/**
 * Una sesion de medicion del tiempo de registro (indicador TPR). El servidor
 * sella el inicio al abrirse el formulario y el fin dentro de la misma
 * transaccion que guarda la cita o el paciente; el navegador nunca informa
 * una duracion.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicionRegistro {

    /**
     * Por debajo de este umbral es casi seguro que no se registro realmente
     * (doble clic, formulario autocompletado): se marca para revision humana,
     * pero no se descarta automaticamente.
     */
    public static final Duration UMBRAL_SOSPECHOSO = Duration.ofSeconds(5);

    private Long id;
    private TipoMedicion tipo;
    private CanalMedicion canal;
    private EstadoMedicion estado;
    private boolean sospechosa;
    private Long usuarioId;
    private Long pacienteId;
    private Long entidadId;
    private Instant inicio;
    private Instant fin;
    private Long capturadoPor;
    private String observacion;

    public static MedicionRegistro iniciar(TipoMedicion tipo, CanalMedicion canal, Long usuarioId, Instant ahora) {
        return MedicionRegistro.builder()
                .tipo(tipo)
                .canal(canal)
                .estado(EstadoMedicion.EN_CURSO)
                .usuarioId(usuarioId)
                .inicio(ahora)
                .build();
    }

    /**
     * Ficha de registro de tiempos del pretest: el investigador transcribe la
     * hora de inicio y de fin observadas en el proceso manual.
     */
    public static MedicionRegistro manual(TipoMedicion tipo, Long pacienteId, Instant inicio, Instant fin,
                                          Long investigadorId, String observacion) {
        if (inicio == null || fin == null || !fin.isAfter(inicio)) {
            throw new ValidacionDeNegocioException("La hora de fin debe ser posterior a la hora de inicio");
        }
        MedicionRegistro medicion = MedicionRegistro.builder()
                .tipo(tipo)
                .canal(CanalMedicion.MANUAL)
                .estado(EstadoMedicion.COMPLETADA)
                .pacienteId(pacienteId)
                .inicio(inicio)
                .fin(fin)
                .capturadoPor(investigadorId)
                .observacion(observacion)
                .build();
        medicion.sospechosa = medicion.duracion().compareTo(UMBRAL_SOSPECHOSO) < 0;
        return medicion;
    }

    /**
     * Cierra la sesion al persistir la entidad registrada. Solo puede cerrarla
     * el mismo usuario que la abrio y una sola vez: una sesion reutilizada o
     * ajena invalidaria el tiempo medido.
     */
    public void completar(Long usuarioQueGuarda, TipoMedicion tipoGuardado, Long pacienteId, Long entidadId, Instant ahora) {
        if (estado != EstadoMedicion.EN_CURSO) {
            throw new ValidacionDeNegocioException("La sesion de medicion " + id + " ya no esta en curso (" + estado + ")");
        }
        if (tipo != tipoGuardado) {
            throw new ValidacionDeNegocioException(
                    "La sesion de medicion " + id + " corresponde a " + tipo + ", no a " + tipoGuardado);
        }
        if (usuarioId != null && !usuarioId.equals(usuarioQueGuarda)) {
            throw new ValidacionDeNegocioException("La sesion de medicion " + id + " pertenece a otro usuario");
        }
        this.fin = ahora;
        this.pacienteId = pacienteId;
        this.entidadId = entidadId;
        this.estado = EstadoMedicion.COMPLETADA;
        this.sospechosa = duracion().compareTo(UMBRAL_SOSPECHOSO) < 0;
    }

    public void anular() {
        if (estado != EstadoMedicion.COMPLETADA) {
            throw new ValidacionDeNegocioException(
                    "Solo se puede anular una medicion completada (estado actual: " + estado + ")");
        }
        this.estado = EstadoMedicion.ANULADA;
    }

    public Duration duracion() {
        return fin == null ? null : Duration.between(inicio, fin);
    }
}

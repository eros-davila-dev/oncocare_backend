package com.threepartners.oncologia.domain.estudio;

import com.threepartners.oncologia.domain.shared.exception.ValidacionDeNegocioException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Una necesidad concreta de un usuario (no un mensaje). Es la unidad del
 * indicador NCA = consultas resueltas / total de consultas cerradas.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Consulta {

    private Long id;
    private CanalConsulta canal;
    private String sesionId;
    private Long pacienteId;
    private String intencion;
    private String resumen;
    private ResultadoConsulta resultado;
    private Instant abiertaEn;
    private Instant cerradaEn;
    private Integer tiempoPrimeraRespuestaMs;
    private Integer valoracion;
    private Long resueltaPorUsuarioId;
    private Long capturadoPor;
    private String observacion;

    /**
     * Ficha de registro del sistema del pretest: consulta recibida por
     * WhatsApp, llamada o en persona, y si el personal la resolvio.
     */
    public static Consulta manual(CanalConsulta canal, Long pacienteId, String resumen, boolean resuelta,
                                  Instant cuando, Long investigadorId, String observacion) {
        if (canal == null || canal.esAutomatizado()) {
            throw new ValidacionDeNegocioException(
                    "Las consultas manuales solo admiten los canales WHATSAPP, LLAMADA o PRESENCIAL");
        }
        return Consulta.builder()
                .canal(canal)
                .pacienteId(pacienteId)
                .resumen(resumen)
                .resultado(resuelta ? ResultadoConsulta.RESUELTA_PERSONAL : ResultadoConsulta.NO_RESUELTA)
                .abiertaEn(cuando)
                .cerradaEn(cuando)
                .capturadoPor(investigadorId)
                .observacion(observacion)
                .build();
    }

    public boolean estaAbierta() {
        return resultado == null || resultado == ResultadoConsulta.ESCALADA;
    }

    /**
     * RESUELTA_BOT puede reabrirse como ESCALADA si el usuario valora mal la
     * respuesta; el resto de resultados finales solo admite anulacion.
     */
    public void cerrar(ResultadoConsulta nuevoResultado, Long usuarioId, Instant cuando) {
        if (!estaAbierta() && resultado != ResultadoConsulta.RESUELTA_BOT) {
            throw new ValidacionDeNegocioException("La consulta " + id + " ya esta cerrada como " + resultado);
        }
        this.resultado = nuevoResultado;
        this.cerradaEn = nuevoResultado.esFinal() ? cuando : null;
        if (nuevoResultado == ResultadoConsulta.RESUELTA_PERSONAL) {
            this.resueltaPorUsuarioId = usuarioId;
        }
    }

    public void anular() {
        if (resultado == ResultadoConsulta.ANULADA) {
            throw new ValidacionDeNegocioException("La consulta " + id + " ya esta anulada");
        }
        this.resultado = ResultadoConsulta.ANULADA;
    }
}

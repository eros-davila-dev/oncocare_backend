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
 * Una necesidad concreta de un usuario (no un mensaje). Es la unidad del
 * indicador NCA = consultas resueltas / total de consultas cerradas.
 *
 * Ciclo de vida en el chatbot:
 * abierta (resultado null) -> RESUELTA_BOT | ESCALADA | NO_RESUELTA (abandono)
 * RESUELTA_BOT -> abierta otra vez si el usuario reformula, o ESCALADA si la valora mal
 * ESCALADA -> RESUELTA_PERSONAL | NO_RESUELTA (sin respuesta del personal a tiempo)
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
    @Builder.Default
    private int turnos = 1;
    private Instant ultimaActividadEn;
    private String notaResolucion;

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
                .ultimaActividadEn(cuando)
                .capturadoPor(investigadorId)
                .observacion(observacion)
                .build();
    }

    /** Nueva necesidad detectada en una conversacion del chatbot. */
    public static Consulta abrir(CanalConsulta canal, String sesionId, Long pacienteId, String intencion, String resumen,
                                 Instant inicio, Instant primeraRespuesta) {
        return Consulta.builder()
                .canal(canal)
                .sesionId(sesionId)
                .pacienteId(pacienteId)
                .intencion(intencion)
                .resumen(resumen)
                .abiertaEn(inicio)
                .ultimaActividadEn(primeraRespuesta)
                .tiempoPrimeraRespuestaMs((int) Duration.between(inicio, primeraRespuesta).toMillis())
                .turnos(1)
                .build();
    }

    public boolean estaAbierta() {
        return resultado == null || resultado == ResultadoConsulta.ESCALADA;
    }

    /** Abierta y todavia a cargo del bot (no escalada). */
    public boolean enManosDelBot() {
        return resultado == null;
    }

    public void registrarTurno(Instant cuando) {
        this.turnos++;
        this.ultimaActividadEn = cuando;
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
        this.ultimaActividadEn = cuando;
        if (nuevoResultado == ResultadoConsulta.RESUELTA_PERSONAL) {
            this.resueltaPorUsuarioId = usuarioId;
        }
    }

    /**
     * El usuario vuelve a preguntar lo mismo poco despues de una respuesta
     * del bot: esa respuesta no lo resolvio. La consulta deja de contar como
     * resuelta hasta que se cierre de nuevo.
     */
    public void reabrirPorReformulacion(Instant cuando) {
        if (resultado != ResultadoConsulta.RESUELTA_BOT) {
            throw new ValidacionDeNegocioException("Solo se reabre una consulta resuelta por el bot");
        }
        this.resultado = null;
        this.cerradaEn = null;
        registrarTurno(cuando);
    }

    public void escalar(Instant cuando) {
        cerrar(ResultadoConsulta.ESCALADA, null, cuando);
    }

    /**
     * Valoracion explicita del usuario. Un pulgar abajo sobre una respuesta
     * del bot significa que no lo resolvio: pasa al personal.
     */
    public void valorar(int valor, Instant cuando) {
        if (valor != 1 && valor != -1) {
            throw new ValidacionDeNegocioException("La valoracion debe ser 1 o -1");
        }
        this.valoracion = valor;
        if (valor == -1 && resultado == ResultadoConsulta.RESUELTA_BOT) {
            escalar(cuando);
        }
    }

    public void resolverPorPersonal(boolean resuelta, Long usuarioId, String nota, Instant cuando) {
        if (resultado != ResultadoConsulta.ESCALADA) {
            throw new ValidacionDeNegocioException("Solo se resuelven desde la bandeja las consultas escaladas");
        }
        this.notaResolucion = nota;
        cerrar(resuelta ? ResultadoConsulta.RESUELTA_PERSONAL : ResultadoConsulta.NO_RESUELTA, usuarioId, cuando);
    }

    public void anular() {
        if (resultado == ResultadoConsulta.ANULADA) {
            throw new ValidacionDeNegocioException("La consulta " + id + " ya esta anulada");
        }
        this.resultado = ResultadoConsulta.ANULADA;
    }
}

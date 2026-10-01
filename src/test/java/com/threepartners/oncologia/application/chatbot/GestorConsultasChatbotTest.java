package com.threepartners.oncologia.application.chatbot;

import com.threepartners.oncologia.domain.chatbot.Intencion;
import com.threepartners.oncologia.domain.chatbot.ResultadoAccion;
import com.threepartners.oncologia.domain.estudio.CanalConsulta;
import com.threepartners.oncologia.domain.estudio.Consulta;
import com.threepartners.oncologia.domain.estudio.ConsultaRepositoryPort;
import com.threepartners.oncologia.domain.estudio.PeriodoMedicion;
import com.threepartners.oncologia.domain.estudio.ResultadoConsulta;
import com.threepartners.oncologia.domain.notificacion.NotificadorExternoPort;
import com.threepartners.oncologia.domain.shared.CriterioPaginacion;
import com.threepartners.oncologia.domain.shared.Pagina;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reglas que deciden el desenlace de cada consulta del chatbot (indicador
 * NCA), probadas sobre un repositorio en memoria y un reloj controlable.
 */
class GestorConsultasChatbotTest {

    private final RepositorioEnMemoria repositorio = new RepositorioEnMemoria();
    private final List<String> avisos = new ArrayList<>();
    private final RelojMovil reloj = new RelojMovil(Instant.parse("2026-10-05T15:00:00Z"));
    private GestorConsultasChatbot gestor;

    @BeforeEach
    void setUp() {
        NotificadorExternoPort notificador = (ruta, payload) -> avisos.add(ruta + payload);
        gestor = new GestorConsultasChatbot(repositorio, notificador, reloj);
    }

    private Consulta turno(Intencion intencion, ResultadoAccion resultado) {
        Instant inicio = reloj.instant();
        reloj.avanzar(Duration.ofSeconds(2));
        return gestor.registrarTurno("s1", CanalConsulta.CHATBOT_WEB, null, intencion, resultado, "mensaje", inicio);
    }

    @Test
    void unaPreguntaRespondidaConInformacionOficialQuedaResueltaPorElBot() {
        Consulta c = turno(Intencion.GENERAL_QUERY, ResultadoAccion.INFORMATIVA);

        assertThat(c.getResultado()).isEqualTo(ResultadoConsulta.RESUELTA_BOT);
        assertThat(c.getTiempoPrimeraRespuestaMs()).isEqualTo(2000);
    }

    @Test
    void unTramiteQuePideDatosSigueAbiertoYSeResuelveEnElMismoRegistro() {
        Consulta primera = turno(Intencion.BOOK_APPOINTMENT, ResultadoAccion.REQUIERE_DATOS);
        assertThat(primera.getResultado()).isNull();

        Consulta segunda = turno(Intencion.BOOK_APPOINTMENT, ResultadoAccion.EXITO);

        assertThat(segunda.getId()).isEqualTo(primera.getId());
        assertThat(segunda.getResultado()).isEqualTo(ResultadoConsulta.RESUELTA_BOT);
        assertThat(segunda.getTurnos()).isEqualTo(2);
        assertThat(repositorio.todas()).hasSize(1);
    }

    @Test
    void unaPreguntaIntercaladaNoCierraElTramiteEnCurso() {
        Consulta tramite = turno(Intencion.BOOK_APPOINTMENT, ResultadoAccion.REQUIERE_DATOS);

        Consulta intercalada = turno(Intencion.GENERAL_QUERY, ResultadoAccion.INFORMATIVA);

        assertThat(intercalada.getId()).isEqualTo(tramite.getId());
        assertThat(intercalada.getResultado()).isNull();
    }

    @Test
    void cambiarDeNecesidadCierraLaAnteriorComoNoResuelta() {
        Consulta abandonada = turno(Intencion.CANCEL_APPOINTMENT, ResultadoAccion.REQUIERE_DATOS);

        Consulta nueva = turno(Intencion.CHECK_APPOINTMENT, ResultadoAccion.EXITO);

        assertThat(nueva.getId()).isNotEqualTo(abandonada.getId());
        assertThat(repositorio.porId(abandonada.getId()).getResultado()).isEqualTo(ResultadoConsulta.NO_RESUELTA);
    }

    @Test
    void repetirLaMismaPreguntaAlPocoTiempoReabreLaConsulta() {
        Consulta respondida = turno(Intencion.GENERAL_QUERY, ResultadoAccion.INFORMATIVA);
        reloj.avanzar(Duration.ofMinutes(3));

        Consulta reformulada = gestor.registrarTurno("s1", CanalConsulta.CHATBOT_WEB, null, Intencion.GENERAL_QUERY,
                ResultadoAccion.REQUIERE_DATOS, "otra vez", reloj.instant());

        assertThat(reformulada.getId()).isEqualTo(respondida.getId());
        assertThat(reformulada.getResultado()).isNull();
    }

    @Test
    void laMismaPreguntaMuchoDespuesEsUnaConsultaNueva() {
        Consulta primera = turno(Intencion.GENERAL_QUERY, ResultadoAccion.INFORMATIVA);
        reloj.avanzar(Duration.ofMinutes(30));

        Consulta segunda = turno(Intencion.GENERAL_QUERY, ResultadoAccion.INFORMATIVA);

        assertThat(segunda.getId()).isNotEqualTo(primera.getId());
        assertThat(repositorio.porId(primera.getId()).getResultado()).isEqualTo(ResultadoConsulta.RESUELTA_BOT);
    }

    @Test
    void siElBotNoLoResuelveEnCuatroTurnosLaConsultaPasaAlPersonal() {
        for (int i = 0; i < GestorConsultasChatbot.MAXIMO_TURNOS - 1; i++) {
            assertThat(turno(Intencion.RESCHEDULE_APPOINTMENT, ResultadoAccion.ERROR_NEGOCIO).getResultado()).isNull();
        }

        Consulta escalada = turno(Intencion.RESCHEDULE_APPOINTMENT, ResultadoAccion.ERROR_NEGOCIO);

        assertThat(escalada.getResultado()).isEqualTo(ResultadoConsulta.ESCALADA);
        assertThat(avisos).hasSize(1);
    }

    @Test
    void escalarAvisaAlPersonalSinEnviarElTextoDelPaciente() {
        Consulta c = turno(Intencion.ESCALATE_TO_STAFF, ResultadoAccion.ESCALAR);

        assertThat(c.getResultado()).isEqualTo(ResultadoConsulta.ESCALADA);
        assertThat(avisos).singleElement().asString().doesNotContain("mensaje").contains("consultaId");
    }

    @Test
    void pedirUnaPersonaSinConsultaPreviaCreaUnaYaEscalada() {
        Consulta c = gestor.escalarPorPedidoDelUsuario("s2", CanalConsulta.CHATBOT_WEB, null);

        assertThat(c.getResultado()).isEqualTo(ResultadoConsulta.ESCALADA);
        assertThat(c.getIntencion()).isEqualTo("ESCALATE_TO_STAFF");
    }

    @Test
    void unPulgarAbajoSobreUnaRespuestaDelBotLaEscala() {
        Consulta c = turno(Intencion.GENERAL_QUERY, ResultadoAccion.INFORMATIVA);

        c.valorar(-1, reloj.instant());

        assertThat(c.getResultado()).isEqualTo(ResultadoConsulta.ESCALADA);
        assertThat(c.getValoracion()).isEqualTo(-1);
    }

    /** Reloj que los tests pueden adelantar. */
    static final class RelojMovil extends Clock {
        private Instant ahora;

        RelojMovil(Instant inicio) {
            this.ahora = inicio;
        }

        void avanzar(Duration duracion) {
            ahora = ahora.plus(duracion);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return ahora;
        }
    }

    static final class RepositorioEnMemoria implements ConsultaRepositoryPort {
        private final Map<Long, Consulta> datos = new java.util.LinkedHashMap<>();
        private long secuencia = 0;

        Consulta porId(Long id) {
            return datos.get(id);
        }

        List<Consulta> todas() {
            return List.copyOf(datos.values());
        }

        @Override
        public Consulta guardar(Consulta consulta) {
            if (consulta.getId() == null) {
                consulta.setId(++secuencia);
            }
            datos.put(consulta.getId(), consulta);
            return consulta;
        }

        @Override
        public Optional<Consulta> buscarPorId(Long id) {
            return Optional.ofNullable(datos.get(id));
        }

        @Override
        public Optional<Consulta> buscarUltimaPorSesion(String sesionId) {
            return datos.values().stream()
                    .filter(c -> sesionId.equals(c.getSesionId()))
                    .max(Comparator.comparing(Consulta::getAbiertaEn).thenComparing(Consulta::getId));
        }

        @Override
        public Pagina<Consulta> listarEscaladas(CriterioPaginacion criterio) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Consulta> abiertasSinActividadAntesDe(Instant limite) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Consulta> escaladasSinActividadAntesDe(Instant limite) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Pagina<Consulta> listar(PeriodoMedicion periodo, CanalConsulta canal, ResultadoConsulta resultado,
                                       CriterioPaginacion criterio) {
            throw new UnsupportedOperationException();
        }
    }
}

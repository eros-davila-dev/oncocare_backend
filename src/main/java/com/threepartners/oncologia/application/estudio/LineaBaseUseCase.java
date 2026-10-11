package com.threepartners.oncologia.application.estudio;

import com.threepartners.oncologia.domain.auditoria.event.OperacionAuditadaEvent;
import com.threepartners.oncologia.domain.estudio.Fase;
import com.threepartners.oncologia.domain.estudio.LineaBaseRepositoryPort;
import com.threepartners.oncologia.domain.estudio.LineaBaseSesion;
import com.threepartners.oncologia.domain.shared.exception.ValidacionDeNegocioException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Linea base del pretest a nivel de sesion (tesis v8, opcion B). El pretest se
 * recolecto a mano antes del sistema; aqui solo se guardan los valores por
 * sesion de la hoja SPSS_Independientes del instrumento, con su fuente.
 *
 * La carga de la linea base solo acepta filas del PRETEST (las de postest se
 * ignoran y se informa cuantas fueron). El postest del instrumento se puede
 * importar aparte (importarPostest) cuando se midio en otra instalacion (el
 * servidor de la fundacion): queda rotulado con su origen y nunca se mezcla con lo que el sistema mide ni crea
 * pacientes o eventos.
 */
@Service
@RequiredArgsConstructor
public class LineaBaseUseCase {

    public static final String FUENTE = "Pretest: instrumento aplicado a mano y validado por la fundación";
    public static final String FUENTE_POSTEST = "Postest: medido en el servidor de la fundación e importado a esta instalación";
    private static final DateTimeFormatter FECHA_DMY = DateTimeFormatter.ofPattern("d/M/yyyy");

    private final LineaBaseRepositoryPort repositorio;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public record Resultado(int sesionesCargadas, int filasPostestIgnoradas, List<LineaBaseSesion> sesiones) {
    }

    /** Valores agregados por sesion (sin datos personales): los ve todo el panel. */
    @PreAuthorize("hasAnyRole('ADMIN', 'INVESTIGADOR', 'MEDICO', 'RECEPCIONISTA')")
    @Transactional(readOnly = true)
    public List<LineaBaseSesion> listar(Fase fase) {
        return repositorio.listar(fase);
    }

    /** Postest del instrumento, rotulado como importado: solo filas de postest. */
    @PreAuthorize("hasAnyRole('ADMIN', 'INVESTIGADOR')")
    @Transactional
    public Resultado importarPostest(List<Map<String, String>> filas, Long usuarioId, String ipOrigen) {
        return guardar(filas, Fase.POSTEST, FUENTE_POSTEST, usuarioId, ipOrigen, "ESTUDIO_POSTEST_IMPORTADO");
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'INVESTIGADOR')")
    @Transactional
    public Resultado cargar(List<Map<String, String>> filas, Long usuarioId, String ipOrigen) {
        return guardar(filas, Fase.PRETEST, FUENTE, usuarioId, ipOrigen, "ESTUDIO_LINEA_BASE_CARGADA");
    }

    private Resultado guardar(List<Map<String, String>> filas, Fase fase, String fuente, Long usuarioId, String ipOrigen,
                              String accionAuditoria) {
        Instant ahora = clock.instant();
        List<LineaBaseSesion> sesiones = new ArrayList<>();
        Set<LocalDate> fechas = new HashSet<>();
        int postestIgnoradas = 0;
        int numeroFila = 1;
        for (Map<String, String> fila : filas) {
            numeroFila++;
            if (fila.values().stream().allMatch(v -> v == null || v.isBlank())) {
                continue;
            }
            String etapa = texto(fila, "etapa").toLowerCase(Locale.ROOT);
            String grupo = texto(fila, "grupo");
            if (etapa.isEmpty() && grupo.isEmpty()) {
                continue; // nota al pie del instrumento, no es una sesion
            }
            boolean esPostest = etapa.startsWith("post") || grupo.equals("2");
            boolean esPretest = etapa.startsWith("pre") || grupo.equals("1");
            if (!esPostest && !esPretest) {
                throw new ValidacionDeNegocioException("Fila " + numeroFila + ": no se reconoce la etapa (use Pretest o Postest)");
            }
            if (esPostest != (fase == Fase.POSTEST)) {
                postestIgnoradas++; // filas de la otra etapa: no se mezclan
                continue;
            }
            LocalDate fecha = fecha(texto(fila, "fecha"), numeroFila);
            if (!fechas.add(fecha)) {
                throw new ValidacionDeNegocioException("Fila " + numeroFila + ": la fecha " + fecha + " esta repetida");
            }
            sesiones.add(new LineaBaseSesion(fase, sesiones.size() + 1, fecha,
                    numero(fila, "tpr", numeroFila, null), numero(fila, "ta", numeroFila, 100.0),
                    numero(fila, "nca", numeroFila, 100.0), fuente, ahora));
        }
        if (sesiones.isEmpty()) {
            throw new ValidacionDeNegocioException("El archivo no tiene filas de " + fase.name().toLowerCase(Locale.ROOT)
                    + ". Use la hoja SPSS_Independientes (columnas sesion, grupo, etapa, fecha, tpr, ta, nca)");
        }
        sesiones.sort((a, b) -> a.fecha().compareTo(b.fecha()));
        List<LineaBaseSesion> numeradas = new ArrayList<>();
        for (int i = 0; i < sesiones.size(); i++) {
            LineaBaseSesion s = sesiones.get(i);
            numeradas.add(new LineaBaseSesion(s.fase(), i + 1, s.fecha(), s.tprMin(), s.taPct(), s.ncaPct(), s.fuente(), s.cargadoEn()));
        }
        repositorio.reemplazar(fase, numeradas, usuarioId);
        eventPublisher.publishEvent(OperacionAuditadaEvent.exito(usuarioId, accionAuditoria, "ESTUDIO", null,
                null, "fase=%s;sesiones=%d;otraEtapaIgnoradas=%d".formatted(fase, numeradas.size(), postestIgnoradas), ipOrigen));
        return new Resultado(numeradas.size(), postestIgnoradas, numeradas);
    }

    private static String texto(Map<String, String> fila, String columna) {
        String valor = fila.get(columna);
        return valor == null ? "" : valor.strip();
    }

    private static LocalDate fecha(String valor, int fila) {
        try {
            return valor.contains("/") ? LocalDate.parse(valor, FECHA_DMY) : LocalDate.parse(valor);
        } catch (DateTimeParseException e) {
            throw new ValidacionDeNegocioException("Fila " + fila + ": fecha invalida '" + valor + "' (use dd/mm/aaaa)");
        }
    }

    private static Double numero(Map<String, String> fila, String columna, int numeroFila, Double maximo) {
        String valor = texto(fila, columna).replace(',', '.');
        if (valor.isEmpty()) {
            return null;
        }
        try {
            double n = Double.parseDouble(valor);
            if (n < 0 || (maximo != null && n > maximo)) {
                throw new ValidacionDeNegocioException("Fila " + numeroFila + ": " + columna + " fuera de rango (" + valor + ")");
            }
            return Math.round(n * 100) / 100.0;
        } catch (NumberFormatException e) {
            throw new ValidacionDeNegocioException("Fila " + numeroFila + ": " + columna + " no es un numero (" + valor + ")");
        }
    }
}

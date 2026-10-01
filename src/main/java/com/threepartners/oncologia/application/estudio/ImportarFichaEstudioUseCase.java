package com.threepartners.oncologia.application.estudio;

import com.threepartners.oncologia.application.estudio.FichasEstudio.ErrorFila;
import com.threepartners.oncologia.application.estudio.FichasEstudio.FichaAsistencia;
import com.threepartners.oncologia.application.estudio.FichasEstudio.FichaConsulta;
import com.threepartners.oncologia.application.estudio.FichasEstudio.FichaTiempo;
import com.threepartners.oncologia.application.estudio.FichasEstudio.ResultadoImportacion;
import com.threepartners.oncologia.application.estudio.FichasEstudio.TipoFicha;
import com.threepartners.oncologia.domain.auditoria.event.OperacionAuditadaEvent;
import com.threepartners.oncologia.domain.cita.Cita;
import com.threepartners.oncologia.domain.cita.CitaRepositoryPort;
import com.threepartners.oncologia.domain.estudio.CanalConsulta;
import com.threepartners.oncologia.domain.estudio.Consulta;
import com.threepartners.oncologia.domain.estudio.ConsultaRepositoryPort;
import com.threepartners.oncologia.domain.estudio.MedicionRegistro;
import com.threepartners.oncologia.domain.estudio.MedicionRegistroRepositoryPort;
import com.threepartners.oncologia.domain.estudio.ParticipanteEstudio;
import com.threepartners.oncologia.domain.estudio.ParticipanteEstudioRepositoryPort;
import com.threepartners.oncologia.domain.estudio.TipoMedicion;
import com.threepartners.oncologia.domain.paciente.Paciente;
import com.threepartners.oncologia.domain.paciente.PacienteRepositoryPort;
import com.threepartners.oncologia.domain.shared.exception.DomainException;
import com.threepartners.oncologia.domain.shared.exception.ValidacionDeNegocioException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Importa las fichas del pretest desde la hoja de calculo que llenaba la
 * fundacion. Primero valida todas las filas; solo si ninguna tiene errores (y
 * se pidio aplicar) las guarda, en una sola transaccion. Asi se puede
 * "previsualizar" sin efectos y nunca queda una ficha importada a medias.
 *
 * Las filas llegan como mapas columna -> texto con los encabezados ya
 * normalizados (minusculas, sin tildes, espacios como "_"); leer el archivo
 * es responsabilidad del adaptador de entrada.
 */
@Service
@RequiredArgsConstructor
public class ImportarFichaEstudioUseCase {

    public static final int MAXIMO_FILAS = 5_000;
    private static final int PRIMERA_FILA_DE_DATOS = 2;
    private static final Pattern CODIGO_PARTICIPANTE = Pattern.compile("^[Pp]\\d+$");
    private static final List<DateTimeFormatter> FORMATOS_FECHA = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("d-M-yyyy"));
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("H:mm[:ss]");

    /**
     * Encabezados alternativos (ya normalizados) que se aceptan como sinonimos:
     * incluyen los nombres literales de las fichas del Anexo 2 de la tesis y
     * los de las hojas de calculo de la fundacion.
     */
    private static final Map<String, String> ALIAS_ENCABEZADOS = Map.ofEntries(
            Map.entry("documento", "paciente"),
            Map.entry("dni", "paciente"),
            Map.entry("documento_identidad", "paciente"),
            Map.entry("codigo", "paciente"),
            Map.entry("participante", "paciente"),
            Map.entry("fecha_de_cita", "fecha"),
            Map.entry("fecha_cita", "fecha"),
            Map.entry("inicio", "hora_inicio"),
            Map.entry("hora_de_inicio", "hora_inicio"),
            Map.entry("fin", "hora_fin"),
            Map.entry("hora_de_fin", "hora_fin"),
            Map.entry("asistencia", "asistio"),
            Map.entry("consulta_recibida", "consulta"),
            Map.entry("tipo_de_consulta", "tipo_consulta"),
            Map.entry("canal", "medio"));

    private final PreparadorFichasEstudio preparador;
    private final ParticipanteEstudioRepositoryPort participanteRepositoryPort;
    private final PacienteRepositoryPort pacienteRepositoryPort;
    private final MedicionRegistroRepositoryPort medicionRegistroRepositoryPort;
    private final CitaRepositoryPort citaRepositoryPort;
    private final ConsultaRepositoryPort consultaRepositoryPort;
    private final ApplicationEventPublisher eventPublisher;

    /** Columnas esperadas por ficha (las marcadas con ? son opcionales). Se usan tambien para la plantilla. */
    public static List<String> columnas(TipoFicha ficha) {
        return switch (ficha) {
            case TIEMPOS -> List.of("paciente", "fecha", "hora_inicio", "hora_fin", "tipo?", "observacion?");
            case ASISTENCIAS -> List.of("paciente", "fecha", "hora?", "asistio", "tipo_consulta?");
            case CONSULTAS -> List.of("fecha", "hora?", "medio", "paciente?", "consulta", "resuelta", "observacion?");
        };
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'INVESTIGADOR')")
    @Transactional
    public ResultadoImportacion ejecutar(TipoFicha ficha, List<Map<String, String>> filas, boolean aplicar,
                                         Long investigadorId, String ipOrigen) {
        if (filas.isEmpty()) {
            throw new ValidacionDeNegocioException("El archivo no contiene filas de datos");
        }
        if (filas.size() > MAXIMO_FILAS) {
            throw new ValidacionDeNegocioException("El archivo supera el maximo de " + MAXIMO_FILAS + " filas");
        }
        filas = filas.stream().map(ImportarFichaEstudioUseCase::aplicarAlias).toList();
        validarEncabezados(ficha, filas.getFirst().keySet());

        List<ErrorFila> errores = new ArrayList<>();
        List<Runnable> guardados = new ArrayList<>();
        Set<String> clavesVistas = new HashSet<>();

        for (int i = 0; i < filas.size(); i++) {
            int numeroFila = i + PRIMERA_FILA_DE_DATOS;
            Map<String, String> fila = filas.get(i);
            if (fila.values().stream().allMatch(v -> v == null || v.isBlank())) {
                continue;
            }
            try {
                switch (ficha) {
                    case TIEMPOS -> prepararTiempo(fila, investigadorId, clavesVistas, guardados::add);
                    case ASISTENCIAS -> prepararAsistencia(fila, investigadorId, clavesVistas, guardados::add);
                    case CONSULTAS -> prepararConsulta(fila, investigadorId, guardados::add);
                }
            } catch (DomainException e) {
                errores.add(new ErrorFila(numeroFila, e.getMessage()));
            }
        }

        boolean aplicado = aplicar && errores.isEmpty() && !guardados.isEmpty();
        if (aplicado) {
            guardados.forEach(Runnable::run);
            eventPublisher.publishEvent(OperacionAuditadaEvent.exito(investigadorId, "ESTUDIO_FICHA_IMPORTADA",
                    "FICHA_" + ficha.name(), null, null, "filas=" + guardados.size(), ipOrigen));
        }
        return new ResultadoImportacion(ficha, filas.size(), guardados.size(), errores, aplicado);
    }

    private void prepararTiempo(Map<String, String> fila, Long investigadorId, Set<String> claves,
                                Consumer<Runnable> guardar) {
        var datos = new FichaTiempo(
                resolverPaciente(obligatorio(fila, "paciente")),
                tipoMedicion(fila.get("tipo")),
                fecha(obligatorio(fila, "fecha")),
                hora(obligatorio(fila, "hora_inicio")),
                hora(obligatorio(fila, "hora_fin")),
                opcional(fila, "observacion"));
        MedicionRegistro medicion = preparador.prepararTiempo(datos, investigadorId);
        exigirUnica(claves, "T|%d|%s|%s".formatted(datos.pacienteId(), medicion.getTipo(), medicion.getInicio()));
        guardar.accept(() -> medicionRegistroRepositoryPort.guardar(medicion));
    }

    private void prepararAsistencia(Map<String, String> fila, Long investigadorId, Set<String> claves,
                                    Consumer<Runnable> guardar) {
        var datos = new FichaAsistencia(
                resolverPaciente(obligatorio(fila, "paciente")),
                fecha(obligatorio(fila, "fecha")),
                horaOpcional(fila.get("hora")),
                opcional(fila, "tipo_consulta"),
                siNo(obligatorio(fila, "asistio"), "asistio"));
        Cita cita = preparador.prepararAsistencia(datos, investigadorId);
        exigirUnica(claves, "A|%d|%s|%s".formatted(datos.pacienteId(), cita.getFecha(), cita.getHora()));
        guardar.accept(() -> citaRepositoryPort.guardar(cita));
    }

    private void prepararConsulta(Map<String, String> fila, Long investigadorId, Consumer<Runnable> guardar) {
        String paciente = opcional(fila, "paciente");
        var datos = new FichaConsulta(
                fecha(obligatorio(fila, "fecha")),
                horaOpcional(fila.get("hora")),
                canal(obligatorio(fila, "medio")),
                paciente != null ? resolverPaciente(paciente) : null,
                obligatorio(fila, "consulta"),
                siNo(obligatorio(fila, "resuelta"), "resuelta"),
                opcional(fila, "observacion"));
        Consulta consulta = preparador.prepararConsulta(datos, investigadorId);
        guardar.accept(() -> consultaRepositoryPort.guardar(consulta));
    }

    /**
     * Acepta el codigo de participante (P07) o el documento de identidad: las
     * hojas originales de la fundacion usan el DNI, las fichas del estudio el
     * codigo.
     */
    private Long resolverPaciente(String identificador) {
        if (CODIGO_PARTICIPANTE.matcher(identificador).matches()) {
            return participanteRepositoryPort.buscarPorCodigo(identificador.toUpperCase(Locale.ROOT))
                    .map(ParticipanteEstudio::getPacienteId)
                    .orElseThrow(() -> new ValidacionDeNegocioException("No existe el participante " + identificador));
        }
        return pacienteRepositoryPort.buscarPorDocumento(identificador)
                .map(Paciente::getId)
                .orElseThrow(() -> new ValidacionDeNegocioException(
                        "No existe un paciente con documento " + identificador));
    }

    private static Map<String, String> aplicarAlias(Map<String, String> fila) {
        Map<String, String> resultado = new java.util.LinkedHashMap<>();
        fila.forEach((columna, valor) -> resultado.putIfAbsent(ALIAS_ENCABEZADOS.getOrDefault(columna, columna), valor));
        return resultado;
    }

    private static void validarEncabezados(TipoFicha ficha, Set<String> presentes) {
        List<String> faltantes = columnas(ficha).stream()
                .filter(c -> !c.endsWith("?"))
                .filter(c -> !presentes.contains(c))
                .toList();
        if (!faltantes.isEmpty()) {
            throw new ValidacionDeNegocioException(
                    "Faltan columnas obligatorias en el archivo: " + String.join(", ", faltantes)
                            + ". Descargue la plantilla para ver el formato esperado.");
        }
    }

    private static void exigirUnica(Set<String> claves, String clave) {
        if (!claves.add(clave)) {
            throw new ValidacionDeNegocioException("Fila duplicada dentro del mismo archivo");
        }
    }

    private static String obligatorio(Map<String, String> fila, String columna) {
        String valor = opcional(fila, columna);
        if (valor == null) {
            throw new ValidacionDeNegocioException("La columna '" + columna + "' es obligatoria");
        }
        return valor;
    }

    private static String opcional(Map<String, String> fila, String columna) {
        String valor = fila.get(columna);
        return valor == null || valor.isBlank() ? null : valor.strip();
    }

    static LocalDate fecha(String texto) {
        for (DateTimeFormatter formato : FORMATOS_FECHA) {
            try {
                return LocalDate.parse(texto, formato);
            } catch (DateTimeParseException ignorada) {
                // se prueba el siguiente formato
            }
        }
        throw new ValidacionDeNegocioException("Fecha invalida '" + texto + "' (use AAAA-MM-DD o DD/MM/AAAA)");
    }

    static LocalTime hora(String texto) {
        try {
            return LocalTime.parse(texto, FORMATO_HORA);
        } catch (DateTimeParseException e) {
            throw new ValidacionDeNegocioException("Hora invalida '" + texto + "' (use HH:MM en formato 24 horas)");
        }
    }

    private static LocalTime horaOpcional(String texto) {
        return texto == null || texto.isBlank() ? null : hora(texto.strip());
    }

    static boolean siNo(String texto, String columna) {
        String normalizado = sinTildes(texto).toUpperCase(Locale.ROOT);
        return switch (normalizado) {
            case "SI", "S", "1", "TRUE", "VERDADERO", "X" -> true;
            case "NO", "N", "0", "FALSE", "FALSO" -> false;
            default -> throw new ValidacionDeNegocioException(
                    "Valor invalido '" + texto + "' en la columna '" + columna + "' (use SI o NO)");
        };
    }

    private static CanalConsulta canal(String texto) {
        String normalizado = sinTildes(texto).toUpperCase(Locale.ROOT).replace(' ', '_');
        return switch (normalizado) {
            case "WHATSAPP", "WSP", "WASAP" -> CanalConsulta.WHATSAPP;
            case "LLAMADA", "TELEFONO", "TELEFONICA" -> CanalConsulta.LLAMADA;
            case "PRESENCIAL", "VENTANILLA", "RECEPCION" -> CanalConsulta.PRESENCIAL;
            default -> throw new ValidacionDeNegocioException(
                    "Medio invalido '" + texto + "' (use WHATSAPP, LLAMADA o PRESENCIAL)");
        };
    }

    private static TipoMedicion tipoMedicion(String texto) {
        if (texto == null || texto.isBlank()) {
            return TipoMedicion.REGISTRO_CITA;
        }
        String normalizado = sinTildes(texto).toUpperCase(Locale.ROOT).replace(' ', '_');
        return switch (normalizado) {
            case "REGISTRO_CITA", "CITA" -> TipoMedicion.REGISTRO_CITA;
            case "REGISTRO_PACIENTE", "PACIENTE", "ALTA" -> TipoMedicion.REGISTRO_PACIENTE;
            case "ACTUALIZACION_PACIENTE", "ACTUALIZACION" -> TipoMedicion.ACTUALIZACION_PACIENTE;
            default -> throw new ValidacionDeNegocioException(
                    "Tipo de registro invalido '" + texto + "' (use CITA, PACIENTE o ACTUALIZACION)");
        };
    }

    private static String sinTildes(String texto) {
        return Normalizer.normalize(texto.strip(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }
}

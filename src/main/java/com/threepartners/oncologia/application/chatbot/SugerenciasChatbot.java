package com.threepartners.oncologia.application.chatbot;

import com.threepartners.oncologia.domain.chatbot.ContextoUsuarioChatbot;
import com.threepartners.oncologia.domain.chatbot.Intencion;
import com.threepartners.oncologia.domain.chatbot.InterpretacionChatbot;
import com.threepartners.oncologia.domain.chatbot.PreguntaFrecuente;
import com.threepartners.oncologia.domain.chatbot.PreguntaFrecuenteRepositoryPort;
import com.threepartners.oncologia.domain.chatbot.ResultadoAccion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/**
 * Botones de respuesta rapida del chatbot: el paciente toca una opcion en vez
 * de escribir. Las decide el backend (no el modelo) segun lo que acaba de
 * pasar, asi que siempre son acciones validas. Cada sugerencia es el texto
 * que se envia como mensaje, de modo que pasa por el mismo flujo (y cuenta
 * igual en el NCA) que si el paciente lo hubiera escrito.
 *
 * Las preguntas frecuentes activas tambien se ofrecen: su respuesta sale de
 * la informacion oficial, que es justo lo que el chatbot resuelve sin derivar.
 */
@Component
@RequiredArgsConstructor
public class SugerenciasChatbot {

    static final int MAXIMO = 6;
    static final int MAXIMO_PREGUNTAS_FRECUENTES = 3;

    static final List<String> ESPECIALIDADES =
            List.of("Oncología clínica", "Oncología quirúrgica", "Radioterapia", "Cuidados paliativos");

    private static final String VER_CITAS = "Ver mis citas";
    private static final String AGENDAR = "Quiero agendar una cita";
    private static final String CONFIRMAR = "Confirmar mi próxima cita";
    private static final String REPROGRAMAR = "Reprogramar mi cita";
    private static final String CANCELAR = "Cancelar mi cita";
    private static final String CREAR_CUENTA = "¿Cómo creo mi cuenta?";

    private final PreguntaFrecuenteRepositoryPort preguntasFrecuentes;

    /** Al abrir el chat (o tras una respuesta informativa). */
    public List<String> iniciales(ContextoUsuarioChatbot contexto) {
        List<String> base = contexto.autenticado() && contexto.perfilPacienteCompleto()
                ? List.of(VER_CITAS, CONFIRMAR, AGENDAR)
                : List.of(AGENDAR, CREAR_CUENTA);
        return combinar(base, true);
    }

    /** Despues de un turno: el siguiente paso natural segun la intencion y lo que paso. */
    public List<String> despues(InterpretacionChatbot interpretacion, ResultadoAccion resultado,
                                ContextoUsuarioChatbot contexto) {
        Intencion intencion = interpretacion.intencion();
        return switch (resultado) {
            // Lo atiende el personal: no se le ofrece seguir con otra cosa en la misma consulta.
            case ESCALAR -> List.of();
            case REQUIERE_SESION -> combinar(List.of(CREAR_CUENTA), true);
            case REQUIERE_DATOS -> intencion == Intencion.BOOK_APPOINTMENT && interpretacion.entidad("especialidad") == null
                    ? ESPECIALIDADES
                    : List.of();
            case EXITO -> switch (intencion) {
                case CHECK_APPOINTMENT -> List.of(CONFIRMAR, REPROGRAMAR, CANCELAR);
                case BOOK_APPOINTMENT, CONFIRM_APPOINTMENT, CANCEL_APPOINTMENT, RESCHEDULE_APPOINTMENT ->
                        combinar(List.of(VER_CITAS), true);
                default -> iniciales(contexto);
            };
            case ERROR_NEGOCIO -> contexto.autenticado() ? List.of(VER_CITAS) : List.of();
            case INFORMATIVA -> iniciales(contexto);
        };
    }

    private List<String> combinar(List<String> base, boolean conPreguntasFrecuentes) {
        LinkedHashSet<String> todas = new LinkedHashSet<>(base);
        if (conPreguntasFrecuentes) {
            try {
                preguntasFrecuentes.listarActivas().stream()
                        .map(PreguntaFrecuente::getPregunta)
                        .filter(p -> p != null && !p.isBlank())
                        .filter(p -> todas.stream().noneMatch(s -> parecidas(s, p)))
                        .limit(MAXIMO_PREGUNTAS_FRECUENTES)
                        .forEach(todas::add);
            } catch (RuntimeException e) {
                // Sin preguntas frecuentes las sugerencias base siguen sirviendo.
            }
        }
        return new ArrayList<>(todas).subList(0, Math.min(MAXIMO, todas.size()));
    }

    /**
     * "¿Cómo creo mi cuenta?" y "¿Cómo creo mi cuenta de paciente?" son el
     * mismo boton: se compara sin tildes, mayusculas ni signos.
     */
    static boolean parecidas(String a, String b) {
        String x = normalizar(a);
        String y = normalizar(b);
        return !x.isEmpty() && !y.isEmpty() && (x.contains(y) || y.contains(x));
    }

    private static String normalizar(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9 ]", "")
                .replaceAll("\\s+", " ")
                .strip();
    }
}

package com.threepartners.oncologia.infrastructure.out.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.threepartners.oncologia.domain.chatbot.ContextoUsuarioChatbot;
import com.threepartners.oncologia.domain.chatbot.ConversacionChatbot;
import com.threepartners.oncologia.domain.chatbot.GeminiPort;
import com.threepartners.oncologia.domain.chatbot.Intencion;
import com.threepartners.oncologia.domain.chatbot.InterpretacionChatbot;
import com.threepartners.oncologia.domain.chatbot.PreguntaFrecuente;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Adaptador de infraestructura hacia la API publica de Gemini (Generative
 * Language API, https://ai.google.dev). Es el UNICO lugar del sistema que
 * conoce el formato HTTP de Gemini: el resto del backend solo ve el puerto
 * GeminiPort y el registro InterpretacionChatbot (seccion 13).
 *
 * Gemini se le pide SIEMPRE una salida JSON con un esquema fijo
 * (generationConfig.responseSchema), nunca texto libre a interpretar a mano:
 * asi la intencion detectada es un valor de un enum cerrado, no una
 * adivinanza sobre lenguaje natural.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiRestClientAdapter implements GeminiPort {

    private static final String INSTRUCCION_SISTEMA = """
            Eres el asistente virtual de la Fundacion Oncologica Three Partners (Lima, Peru).
            Ayudas a pacientes con tareas administrativas: registro, agendamiento,
            confirmacion, reprogramacion y cancelacion de citas, y preguntas generales
            sobre el proceso de atencion.

            REGLAS DE SEGURIDAD, OBLIGATORIAS SIN EXCEPCION:
            - NUNCA diagnostiques, interpretes sintomas ni sugieras tratamientos o
              medicamentos. Cualquier pregunta medica (sintomas, efectos de un
              tratamiento, resultados de examenes) se clasifica como
              ESCALATE_TO_STAFF con una respuesta breve indicando que el equipo
              medico le respondera.
            - NUNCA inventes datos que no te dieron en este mensaje (horarios exactos,
              direcciones, precios, nombres de medicos). Responde preguntas generales
              SOLO con la INFORMACION REAL y las PREGUNTAS FRECUENTES de abajo. Si la
              respuesta no esta ahi, clasifica como ESCALATE_TO_STAFF y di que el
              equipo de la fundacion le respondera: un "no se" no resuelve la
              consulta del paciente.
            - NUNCA reveles ni asumas informacion de otro paciente distinto al que
              esta escribiendo.
            - Solo puedes clasificar la intencion en una de estas exactas (usa el
              nombre en ingles tal cual, es un enum del backend):
              REGISTER_PATIENT, BOOK_APPOINTMENT, CHECK_APPOINTMENT,
              RESCHEDULE_APPOINTMENT, CANCEL_APPOINTMENT, CONFIRM_APPOINTMENT,
              GENERAL_QUERY, HELP, ESCALATE_TO_STAFF.
            - Tu no ejecutas ninguna accion: solo interpretas. El backend decide si
              la accion es valida y la ejecuta con datos reales de la base de datos.
              Por eso, si la intencion implica una accion sobre una cita
              (agendar/confirmar/reprogramar/cancelar/consultar), NUNCA inventes el
              resultado (no digas "tu cita quedo confirmada"): limitate a extraer los
              datos necesarios y, si faltan, preguntarlos.

            INFORMACION REAL DEL SISTEMA (unica fuente para GENERAL_QUERY/HELP):
            - Un paciente nuevo se registra en /auth/registro con su correo y
              contrasena, verifica su correo, y luego completa su ficha (documento,
              datos de contacto) para poder agendar.
            - Una vez con sesion iniciada, el paciente ve, confirma, reprograma o
              cancela sus propias citas desde "Mis citas".
            - Para agendar por chat necesitas indicar la especialidad
              (oncologia clinica, oncologia quirurgica, radioterapia o cuidados
              paliativos), la fecha y la hora que prefieres.
            - Los horarios exactos de atencion, la direccion y los documentos que se
              piden en la primera cita presencial los confirma el personal de
              recepcion: no los inventes.

            Formato de salida: SIEMPRE responde solo el JSON pedido, en el campo
            "respuesta" escribe en espanol, tono calido y profesional, breve (2-4
            oraciones como maximo).
            """;

    private final GeminiProperties geminiProperties;
    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;

    @Override
    public InterpretacionChatbot interpretar(String mensajeUsuario, List<ConversacionChatbot> historialReciente, ContextoUsuarioChatbot contexto) {
        if (geminiProperties.apiKey() == null || geminiProperties.apiKey().isBlank()) {
            log.warn("GEMINI_API_KEY no configurada: el chatbot respondera en modo degradado");
            return respuestaNoDisponible();
        }

        try {
            ObjectNode cuerpo = construirCuerpo(mensajeUsuario, historialReciente, contexto);

            String uri = "%s/v1beta/models/%s:generateContent".formatted(geminiProperties.baseUrl(), geminiProperties.model());

            String respuestaCruda = restClientBuilder.build()
                    .post()
                    .uri(uri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("x-goog-api-key", geminiProperties.apiKey())
                    .body(cuerpo)
                    .retrieve()
                    .body(String.class);

            return parsearRespuesta(respuestaCruda);
        } catch (Exception e) {
            log.error("Fallo la llamada a Gemini", e);
            return respuestaNoDisponible();
        }
    }

    private ObjectNode construirCuerpo(String mensajeUsuario, List<ConversacionChatbot> historial, ContextoUsuarioChatbot contexto) {
        ObjectNode raiz = objectMapper.createObjectNode();

        ObjectNode instruccionSistema = raiz.putObject("systemInstruction");
        instruccionSistema.putArray("parts").addObject().put("text", INSTRUCCION_SISTEMA + "\n\n" + contextoComoTexto(contexto));

        var contents = raiz.putArray("contents");
        for (ConversacionChatbot turno : historial) {
            contents.addObject()
                    .put("role", "user")
                    .putArray("parts").addObject().put("text", turno.getMensajeUsuario());
            if (turno.getRespuestaBot() != null) {
                var partsModelo = contents.addObject().put("role", "model").putArray("parts");
                partsModelo.addObject().put("text", turno.getRespuestaBot());
            }
        }
        contents.addObject()
                .put("role", "user")
                .putArray("parts").addObject().put("text", mensajeUsuario);

        ObjectNode generationConfig = raiz.putObject("generationConfig");
        generationConfig.put("responseMimeType", "application/json");
        generationConfig.set("responseSchema", esquemaRespuesta());

        return raiz;
    }

    private String contextoComoTexto(ContextoUsuarioChatbot contexto) {
        return contextoDelUsuario(contexto) + preguntasFrecuentesComoTexto(contexto.preguntasFrecuentes());
    }

    /**
     * Informacion oficial cargada por la fundacion en la intranet: la unica
     * fuente valida para GENERAL_QUERY ademas de las reglas del sistema.
     */
    static String preguntasFrecuentesComoTexto(List<PreguntaFrecuente> preguntas) {
        if (preguntas == null || preguntas.isEmpty()) {
            return "";
        }
        StringBuilder texto = new StringBuilder("\n\nPREGUNTAS FRECUENTES OFICIALES DE LA FUNDACION:\n");
        for (PreguntaFrecuente p : preguntas) {
            texto.append("- P: ").append(p.getPregunta()).append("\n  R: ").append(p.getRespuesta()).append('\n');
        }
        return texto.toString();
    }

    private String contextoDelUsuario(ContextoUsuarioChatbot contexto) {
        if (!contexto.autenticado()) {
            return "El usuario NO ha iniciado sesion. Si pide consultar, agendar, confirmar, reprogramar o "
                    + "cancelar una cita, indicale amablemente que primero debe iniciar sesion (o registrarse si no tiene cuenta).";
        }
        if (!contexto.perfilPacienteCompleto()) {
            return "El usuario inicio sesion como %s pero todavia no completo su ficha de paciente. Si pide gestionar citas, indicale que complete su perfil primero."
                    .formatted(contexto.nombre());
        }
        return "El usuario autenticado se llama %s y ya tiene su ficha de paciente completa: puede agendar, consultar, confirmar, reprogramar y cancelar sus propias citas."
                .formatted(contexto.nombre());
    }

    private ObjectNode esquemaRespuesta() {
        ObjectNode esquema = objectMapper.createObjectNode();
        esquema.put("type", "OBJECT");

        ObjectNode propiedades = esquema.putObject("properties");

        ObjectNode intencion = propiedades.putObject("intencion");
        intencion.put("type", "STRING");
        var enumIntenciones = intencion.putArray("enum");
        for (Intencion valor : Intencion.values()) {
            enumIntenciones.add(valor.name());
        }

        propiedades.putObject("fecha").put("type", "STRING")
                .put("description", "Fecha solicitada en formato YYYY-MM-DD, solo si el usuario la menciono");
        propiedades.putObject("hora").put("type", "STRING")
                .put("description", "Hora solicitada en formato HH:mm (24h), solo si el usuario la menciono");

        ObjectNode especialidad = propiedades.putObject("especialidad");
        especialidad.put("type", "STRING");
        especialidad.putArray("enum").add("ONCOLOGIA_CLINICA").add("ONCOLOGIA_QUIRURGICA").add("RADIOTERAPIA").add("CUIDADOS_PALIATIVOS");

        propiedades.putObject("motivo").put("type", "STRING")
                .put("description", "Motivo de consulta o de cancelacion, si el usuario lo menciono");

        propiedades.putObject("listoParaEjecutar").put("type", "BOOLEAN")
                .put("description", "true solo si ya tienes todos los datos necesarios para que el backend ejecute la accion");

        propiedades.putObject("respuesta").put("type", "STRING")
                .put("description", "Respuesta en espanol para mostrar al usuario");

        esquema.putArray("required").add("intencion").add("listoParaEjecutar").add("respuesta");
        return esquema;
    }

    private InterpretacionChatbot parsearRespuesta(String respuestaCruda) throws Exception {
        JsonNode raiz = objectMapper.readTree(respuestaCruda);
        String textoJson = raiz.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText();
        JsonNode datos = objectMapper.readTree(textoJson);

        Intencion intencion = parsearIntencion(datos.path("intencion").asText(""));
        boolean listo = datos.path("listoParaEjecutar").asBoolean(false);
        String respuesta = datos.path("respuesta").asText("¿Podrias darme mas detalles?");

        Map<String, String> entidades = new HashMap<>();
        agregarSiPresente(datos, "fecha", entidades);
        agregarSiPresente(datos, "hora", entidades);
        agregarSiPresente(datos, "especialidad", entidades);
        agregarSiPresente(datos, "motivo", entidades);

        return new InterpretacionChatbot(intencion, entidades, listo, respuesta);
    }

    private void agregarSiPresente(JsonNode datos, String campo, Map<String, String> entidades) {
        JsonNode valor = datos.path(campo);
        if (!valor.isMissingNode() && !valor.isNull() && !valor.asText().isBlank()) {
            entidades.put(campo, valor.asText());
        }
    }

    private Intencion parsearIntencion(String valor) {
        try {
            return Intencion.valueOf(valor);
        } catch (IllegalArgumentException e) {
            return Intencion.ESCALATE_TO_STAFF;
        }
    }

    private InterpretacionChatbot respuestaNoDisponible() {
        return new InterpretacionChatbot(
                Intencion.ESCALATE_TO_STAFF,
                Map.of(),
                false,
                "En este momento no puedo procesar tu mensaje. Un miembro del equipo te ayudara pronto; "
                        + "tambien puedes llamar directamente a la fundacion.");
    }
}

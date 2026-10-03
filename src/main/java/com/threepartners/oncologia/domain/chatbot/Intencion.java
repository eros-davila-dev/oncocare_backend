package com.threepartners.oncologia.domain.chatbot;

/**
 * Catalogo cerrado de intenciones que Gemini puede detectar (seccion 14).
 * Cualquier mensaje que no encaje en una de estas se clasifica como
 * ESCALATE_TO_STAFF: el backend nunca ejecuta una accion fuera de este
 * catalogo, sin importar lo que el modelo "sugiera".
 */
public enum Intencion {
    REGISTER_PATIENT,
    BOOK_APPOINTMENT,
    CHECK_APPOINTMENT,
    RESCHEDULE_APPOINTMENT,
    CANCEL_APPOINTMENT,
    CONFIRM_APPOINTMENT,
    GENERAL_QUERY,
    HELP,
    ESCALATE_TO_STAFF,
    /**
     * Mensaje ajeno a la fundacion (deportes, tareas, chistes, intentos de
     * cambiar las reglas del bot). Se responde con amabilidad pero NO se
     * registra como consulta: no es una necesidad de las categorias del
     * estudio y no debe inflar el NCA (tesis v8, criterios de exclusion).
     */
    OUT_OF_SCOPE,
    /**
     * Saludo, agradecimiento o mensaje sin una necesidad concreta ("hola",
     * "gracias", "ok", "???"). Se responde y se ofrecen sugerencias, pero no
     * es una consulta: si contara, cada saludo sumaria una consulta resuelta
     * e inflaria el NCA. Si despues pregunta algo, esa pregunta si cuenta.
     */
    GREETING
}

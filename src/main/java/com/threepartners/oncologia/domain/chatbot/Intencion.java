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
    ESCALATE_TO_STAFF
}

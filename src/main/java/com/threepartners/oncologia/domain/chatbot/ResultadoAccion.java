package com.threepartners.oncologia.domain.chatbot;

/**
 * Que paso con la necesidad del usuario en este turno. Es lo que decide el
 * desenlace de la consulta (indicador NCA); por eso lo determina el backend
 * segun lo que realmente ejecuto, nunca el modelo de lenguaje.
 */
public enum ResultadoAccion {
    /** Se ejecuto la accion pedida (agendar, cancelar, consultar citas...). */
    EXITO,
    /** Pregunta general respondida con informacion oficial. */
    INFORMATIVA,
    /** Faltan datos: el bot los pidio y la consulta sigue abierta. */
    REQUIERE_DATOS,
    /** La accion exige iniciar sesion o completar el perfil. */
    REQUIERE_SESION,
    /** Una regla de negocio impidio la accion (horario ocupado, etc.). */
    ERROR_NEGOCIO,
    /** Necesita a una persona (pregunta medica, sin informacion, pedido explicito). */
    ESCALAR
}

package com.threepartners.oncologia.domain.chatbot;

/** De donde sale cada parte de la configuracion que usa el chatbot ahora. */
public enum OrigenConfiguracionGemini {
    /** Guardada por el administrador desde la intranet. */
    INTRANET,
    /** Variables de entorno del servidor (GEMINI_API_KEY, GEMINI_MODEL, GEMINI_MODELS). */
    ENTORNO,
    /** No hay clave en ningun lado: el chatbot responde en modo degradado. */
    SIN_CONFIGURAR
}

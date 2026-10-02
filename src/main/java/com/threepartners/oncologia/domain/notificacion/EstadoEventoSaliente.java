package com.threepartners.oncologia.domain.notificacion;

public enum EstadoEventoSaliente {
    PENDIENTE,
    /** Reclamado por una instancia que lo esta enviando ahora. */
    EN_ENVIO,
    ENVIADO,
    /** Agoto los reintentos: requiere revisar n8n o el workflow. */
    FALLIDO
}

package com.threepartners.oncologia.domain.recordatorio;

public enum EstadoRecordatorio {
    PENDIENTE,
    /** Tomado por n8n para enviarlo; vuelve a PENDIENTE si n8n no informa el resultado. */
    EN_PROCESO,
    ENVIADO,
    FALLIDO,
    /** La cita se cancelo, se reprogramo o ya paso. */
    CANCELADO
}

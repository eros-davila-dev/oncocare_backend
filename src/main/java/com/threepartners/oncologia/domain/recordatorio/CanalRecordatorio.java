package com.threepartners.oncologia.domain.recordatorio;

public enum CanalRecordatorio {
    /** Mensaje con botones enviado por n8n al chat vinculado del paciente. */
    TELEGRAM,
    /** El paciente no tiene Telegram: recepcion lo llama (lista en la agenda). */
    LLAMADA
}

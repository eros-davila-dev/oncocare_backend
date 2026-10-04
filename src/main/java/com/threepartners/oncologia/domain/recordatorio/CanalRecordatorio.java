package com.threepartners.oncologia.domain.recordatorio;

public enum CanalRecordatorio {
    /** Mensaje con botones enviado por n8n al chat vinculado del paciente. */
    TELEGRAM,
    /** El mismo aviso al Telegram del referido (acompanante), si el paciente lo autorizo. */
    TELEGRAM_REFERIDO,
    /** El paciente no tiene Telegram: recepcion lo llama (lista en la agenda). */
    LLAMADA,
    /** Correo al paciente (y copia al referido si lo autorizo); lo envia el backend, no n8n. */
    CORREO
}

package com.threepartners.oncologia.domain.estudio;

public enum CanalConsulta {
    CHATBOT_WEB,
    TELEGRAM,
    /** Canales del pretest, registrados en la ficha del sistema por el investigador. */
    WHATSAPP,
    LLAMADA,
    PRESENCIAL;

    public boolean esAutomatizado() {
        return this == CHATBOT_WEB || this == TELEGRAM;
    }
}

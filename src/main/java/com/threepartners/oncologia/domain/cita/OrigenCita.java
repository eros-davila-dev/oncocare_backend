package com.threepartners.oncologia.domain.cita;

/**
 * Por donde se agendo la cita. Permite separar en los reportes el
 * agendamiento por el personal del autoservicio, y aislar las citas del
 * pretest que el investigador transcribio de las hojas de calculo.
 */
public enum OrigenCita {
    INTRANET,
    PORTAL,
    CHATBOT_WEB,
    TELEGRAM,
    CAPTURA_PRETEST
}

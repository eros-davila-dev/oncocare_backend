package com.threepartners.oncologia.domain.estudio;

import java.util.EnumSet;
import java.util.Set;

public enum CanalMedicion {
    /** Personal de la fundacion usando la intranet. */
    INTRANET,
    /** Paciente en autoservicio desde el portal. */
    PORTAL,
    CHATBOT_WEB,
    TELEGRAM,
    /** Ficha de registro de tiempos llenada por el investigador (pretest). */
    MANUAL;

    /**
     * Canales por defecto del TPR de la hipotesis: compara al personal antes
     * (MANUAL, hojas de calculo) con el personal despues (INTRANET). El
     * autoservicio mide el tiempo del paciente, no del personal, y se reporta
     * aparte.
     */
    public static Set<CanalMedicion> porDefectoParaHipotesis() {
        return EnumSet.of(INTRANET, MANUAL);
    }
}

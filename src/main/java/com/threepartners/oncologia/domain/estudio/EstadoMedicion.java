package com.threepartners.oncologia.domain.estudio;

public enum EstadoMedicion {
    EN_CURSO,
    COMPLETADA,
    /** El formulario se abrio pero nunca se guardo dentro del tiempo maximo. */
    ABANDONADA,
    /** Excluida por el investigador con motivo registrado en correccion_medicion. */
    ANULADA
}

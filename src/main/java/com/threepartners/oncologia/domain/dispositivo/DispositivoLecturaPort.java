package com.threepartners.oncologia.domain.dispositivo;

/**
 * Puerto generico de entrada para registrar una lectura proveniente de un
 * dispositivo externo (IoT o equipo de consultorio). El dominio no sabe ni
 * le importa si la lectura llego por MQTT, REST o un puente serial: los
 * adaptadores de infraestructura (infrastructure/in/device/) son los unicos
 * que conocen el protocolo concreto y todos convergen en este mismo puerto.
 */
public interface DispositivoLecturaPort {

    LecturaDispositivo registrarLectura(LecturaDispositivo lectura);
}

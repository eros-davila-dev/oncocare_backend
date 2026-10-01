package com.threepartners.oncologia.infrastructure.in.device.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.threepartners.oncologia.domain.dispositivo.DispositivoLecturaPort;
import com.threepartners.oncologia.domain.dispositivo.DispositivoRepositoryPort;
import com.threepartners.oncologia.domain.dispositivo.LecturaDispositivo;
import com.threepartners.oncologia.infrastructure.in.rest.CredencialDispositivoHasher;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Adaptador de entrada que se suscribe a un broker MQTT (Eclipse Mosquitto)
 * donde publican los sensores IoT con conectividad de red propia (seccion 14).
 * Deshabilitado por defecto (app.device.mqtt.enabled=false): esta seccion del
 * sistema es un contrato de integracion, no una implementacion contra hardware
 * real concreto, tal como se declara el alcance honesto de la seccion 14.
 * Al recibir un mensaje converge en DispositivoLecturaPort, el mismo puerto
 * que usa DeviceWebhookController, sin que el dominio conozca este protocolo.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.device.mqtt", name = "enabled", havingValue = "true")
public class MqttDeviceListenerAdapter {

    private final MqttDeviceProperties properties;
    private final DispositivoRepositoryPort dispositivoRepositoryPort;
    private final DispositivoLecturaPort dispositivoLecturaPort;
    private final CredencialDispositivoHasher credencialHasher;
    private final ObjectMapper objectMapper;

    private MqttClient client;

    @PostConstruct
    public void conectar() {
        try {
            client = new MqttClient(properties.brokerUrl(), properties.clientId(), new MemoryPersistence());
            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(true);
            client.connect(options);
            client.subscribe(properties.topic(), this::alRecibirMensaje);
            log.info("Suscrito al broker MQTT {} en el topico {}", properties.brokerUrl(), properties.topic());
        } catch (MqttException e) {
            log.error("No se pudo conectar al broker MQTT {}", properties.brokerUrl(), e);
        }
    }

    @PreDestroy
    public void desconectar() {
        try {
            if (client != null && client.isConnected()) {
                client.disconnect();
            }
        } catch (MqttException e) {
            log.warn("Error al desconectar del broker MQTT", e);
        }
    }

    private void alRecibirMensaje(String topic, MqttMessage message) {
        try {
            MensajeDispositivo mensaje = objectMapper.readValue(message.getPayload(), MensajeDispositivo.class);

            dispositivoRepositoryPort.buscarPorCredencialHash(credencialHasher.hash(mensaje.credencialDispositivo()))
                    .ifPresentOrElse(
                            dispositivo -> dispositivoLecturaPort.registrarLectura(LecturaDispositivo.builder()
                                    .dispositivoId(dispositivo.getId())
                                    .pacienteId(mensaje.pacienteId())
                                    .cicloTratamientoId(mensaje.cicloTratamientoId())
                                    .tipoDato(mensaje.tipoDato())
                                    .valor(mensaje.valor())
                                    .build()),
                            () -> log.warn("Mensaje MQTT descartado: credencial de dispositivo no reconocida"));
        } catch (Exception e) {
            log.error("No se pudo procesar el mensaje MQTT recibido en el topico {}", topic, e);
        }
    }

    private record MensajeDispositivo(String credencialDispositivo, Long pacienteId, Long cicloTratamientoId,
                                       String tipoDato, String valor) {
    }
}

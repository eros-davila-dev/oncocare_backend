package com.threepartners.oncologia.infrastructure.in.device.mqtt;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.device.mqtt")
public record MqttDeviceProperties(boolean enabled, String brokerUrl, String clientId, String topic) {
}

package com.smartgarden.integration.mqtt.publisher;

import com.fasterxml.jackson.databind.json.JsonMapper;
//import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartgarden.integration.exception.IntegrationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.MessageChannel;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Publica comandos e configurações para dispositivos IoT via MQTT.
 * Usa o {@code mqttOutboundChannel} configurado em
 * {@link com.smartgarden.integration.config.MqttConfig}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommandMqttPublisher {

  private final MessageChannel mqttOutboundChannel;
  // private final ObjectMapper objectMapper;
  private final JsonMapper mapper = JsonMapper.builder()
      .findAndAddModules()
      .build();

  @Value("${mqtt.topic.publish.command}")
  private String commandTopicTemplate;

  @Value("${mqtt.topic.publish.config}")
  private String configTopicTemplate;

  @Value("${mqtt.qos:1}")
  private int qos;

  /**
   * Publica um comando para o tópico
   * {@code smartgarden/devices/{deviceId}/command}.
   */
  public void publishCommand(String deviceId, Map<String, Object> command) {
    String topic = commandTopicTemplate.formatted(deviceId);
    log.info("MQTT command → [device={}, topic={}, type={}]",
        deviceId, topic, command.get("type"));
    send(topic, command);
  }

  /**
   * Publica uma configuração para o tópico
   * {@code smartgarden/devices/{deviceId}/config}.
   */
  public void publishConfig(String deviceId, Map<String, Object> config) {
    String topic = configTopicTemplate.formatted(deviceId);
    log.info("MQTT config → [device={}, topic={}]", deviceId, topic);
    send(topic, config);
  }

  /* ------------------------------------------------------------------ */

  private void send(String topic, Object payload) {
    try {
      String json = mapper.writeValueAsString(payload);
      mqttOutboundChannel.send(
          MessageBuilder.withPayload(json)
              .setHeader(MqttHeaders.TOPIC, topic)
              .setHeader(MqttHeaders.QOS, qos)
              .build());
    } catch (Exception e) {
      throw new IntegrationException("MQTT publish failed for topic: " + topic, e);
    }
  }
}

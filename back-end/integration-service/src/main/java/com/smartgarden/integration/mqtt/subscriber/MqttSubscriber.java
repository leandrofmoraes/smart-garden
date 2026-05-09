package com.smartgarden.integration.mqtt.subscriber;

import com.fasterxml.jackson.databind.json.JsonMapper;
//import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartgarden.integration.dto.device.DeviceStatusDto;
import com.smartgarden.integration.dto.device.IrrigationReadingDto;
import com.smartgarden.integration.service.DeviceService;
import com.smartgarden.integration.service.ReadingService;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

/**
 * Recebe mensagens MQTT do canal inbound e roteia por tipo de tópico.
 *
 * <p>
 * Responsabilidades:
 * <ul>
 * <li>Parsear o payload JSON</li>
 * <li>Extrair o deviceId do tópico com validação</li>
 * <li>Delegar para o service correto</li>
 * </ul>
 *
 * <p>
 * NÃO publica AMQP diretamente, NÃO monta DTOs complexos,
 * NÃO contém lógica de negócio.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MqttSubscriber {

  private final ReadingService readingService;
  private final DeviceService deviceService;
  // private final ObjectMapper objectMapper;
  private final JsonMapper objectMapper = JsonMapper.builder()
      .findAndAddModules()
      .build();

  @ServiceActivator(inputChannel = "mqttInboundChannel")
  public void handleMqttMessage(Message<?> message) {
    String topic = (String) message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC);
    String payload = message.getPayload().toString();

    if (topic == null) {
      log.warn("MQTT message received with null topic — ignoring");
      return;
    }

    Optional<String> deviceIdOpt = extractDeviceId(topic);
    if (deviceIdOpt.isEmpty()) {
      log.warn("Could not extract deviceId from MQTT topic '{}' — ignoring", topic);
      return;
    }

    String deviceId = deviceIdOpt.get();

    try {
      if (topic.endsWith("/reading")) {
        handleReading(deviceId, payload);
      } else if (topic.endsWith("/status")) {
        handleStatus(deviceId, payload);
      } else if (topic.endsWith("/telemetry")) {
        handleTelemetry(deviceId, payload);
      } else {
        log.warn("No handler registered for MQTT topic '{}' — ignoring", topic);
      }
    } catch (Exception e) {
      log.error("Error processing MQTT message [topic={}, device={}]: {}",
          topic, deviceId, e.getMessage(), e);
    }
  }

  /* ------------------------------------------------------------------ */

  private void handleReading(String deviceId, String payload) throws Exception {
    // Garante que o device seja adicionado ao cache local
    deviceService.registerIfAbsent(deviceId, null);

    IrrigationReadingDto reading = objectMapper.readValue(payload, IrrigationReadingDto.class);
    reading.setDeviceId(deviceId);
    if (reading.getTimestamp() == null) {
      reading.setTimestamp(Instant.now());
    }
    readingService.process(reading);
    log.info("MQTT reading received [device={}, humidity={}]",
        deviceId, reading.getHumidity());
  }

  private void handleStatus(String deviceId, String payload) throws Exception {
    DeviceStatusDto status = objectMapper.readValue(payload, DeviceStatusDto.class);
    status.setDeviceId(deviceId);
    status.setOnline(true);
    status.setLastSeen(Instant.now());
    deviceService.updateStatus(deviceId, status);
    log.info("MQTT status updated [device={}, ip={}, rssi={}]",
        deviceId, status.getIp(), status.getRssi());
  }

  private void handleTelemetry(String deviceId, String payload) {
    deviceService.registerIfAbsent(deviceId, null);
    log.debug("MQTT telemetry received [device={}]", deviceId);
  }

  /* ------------------------------------------------------------------ */

  /**
   * Extrai o deviceId de um tópico no formato:
   * {@code smartgarden/devices/{deviceId}/{type}}
   *
   * @return Optional com o deviceId, ou vazio se o formato for inválido
   */
  private Optional<String> extractDeviceId(String topic) {
    String[] parts = topic.split("/");
    if (parts.length < 4
        || !"smartgarden".equals(parts[0])
        || !"devices".equals(parts[1])
        || parts[2].isBlank()) {
      return Optional.empty();
    }
    return Optional.of(parts[2]);
  }

  @PostConstruct
  public void init() {
    log.info("MqttSubscriber bean initialized and registered for channel mqttInboundChannel");
  }
}

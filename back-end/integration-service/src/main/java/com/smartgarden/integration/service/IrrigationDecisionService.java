package com.smartgarden.integration.service;

import com.smartgarden.integration.cache.DeviceCache;
import com.smartgarden.integration.cache.PlantDeviceCache;
import com.smartgarden.integration.dto.irrigation.IrrigationDecisionDto;
import com.smartgarden.integration.mqtt.publisher.CommandMqttPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Traduz uma {@link IrrigationDecisionDto} recebida via AMQP em um
 * comando MQTT publicado ao dispositivo IoT correto.
 *
 * <p>
 * Responsabilidade única: decidir se publica e montar o payload do comando.
 * Não acessa filas AMQP, não manipula caches de alerta.
 *
 * <p>
 * O campo {@code stopAtHumidity} no payload do comando é o limite de parada:
 * o ESP32/ESP8266 monitora a umidade durante a rega e interrompe ao atingir
 * esse valor — eliminando o risco de loop infinito.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IrrigationDecisionService {

  private final CommandMqttPublisher commandMqttPublisher;
  private final DeviceCache deviceCache;
  private final PlantDeviceCache plantDeviceCache;

  /**
   * Processa a decisão e publica o comando MQTT se {@code shouldIrrigate = true}.
   *
   * @param decision decisão produzida pelo plant-management-service
   */
  public void process(IrrigationDecisionDto decision) {

    if (decision == null) {
      log.warn("Irrigation decision is null, ignoring");
      return;
    }

    String deviceId = decision.getDeviceId();

    // Captura o mapeamento plantId → deviceKey sempre que disponível
    if (decision.getPlantId() != null && !decision.getPlantId().isBlank()
        && deviceId != null && !deviceId.isBlank()) {
      plantDeviceCache.register(decision.getPlantId(), deviceId);
    }

    if (deviceId == null || deviceId.isBlank()) {
      log.warn("Irrigation decision without deviceId, ignoring");
      return;
    }

    log.info("Irrigation decision [plant={}, device={}, shouldIrrigate={}, reason={}, humidity={}]",
        decision.getPlantId(), deviceId, decision.getShouldIrrigate(), decision.getReason(),
        decision.getCurrentHumidity());

    if (!Boolean.TRUE.equals(decision.getShouldIrrigate())) {
      log.debug("shouldIrrigate=false for device={} — no MQTT command published", deviceId);
      return;
    }

    if (!deviceCache.exists(deviceId)) {
      log.warn("Device '{}' not in cache — publishing MQTT command anyway (may be online via MQTT)",
          deviceId);
    }

    publishCommand(deviceId, decision);
  }

  private void publishCommand(String deviceId, IrrigationDecisionDto decision) {

    if (decision.getTargetHumidity() == null) {
      log.warn("Irrigation decision without targetHumidity for device={}, ignoring", deviceId);
      return;
    }

    Map<String, Object> command = new LinkedHashMap<>();
    command.put("type", "IRRIGATE");
    command.put("stopAtHumidity", decision.getTargetHumidity());
    command.put("currentHumidity", decision.getCurrentHumidity());
    command.put("minHumidity", decision.getMinHumidity());
    command.put("maxHumidity", decision.getMaxHumidity());
    command.put("plantId", decision.getPlantId());
    command.put("reason", decision.getReason());

    log.info("MQTT irrigation command → [device={}, stopAt={}]",
        deviceId, decision.getTargetHumidity());

    commandMqttPublisher.publishCommand(deviceId, command);
  }
}

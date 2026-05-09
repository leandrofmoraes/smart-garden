package com.smartgarden.integration.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache em memória que mapeia plantId (UUID) → deviceKey (identificador MQTT).
 * Populado a partir das decisões de irrigação recebidas do
 * plant-management-service.
 * Volátil (reinicia vazio a cada restart do serviço).
 */
@Slf4j
@Component
public class PlantDeviceCache {

  private final ConcurrentHashMap<String, String> plantToDevice = new ConcurrentHashMap<>();

  public void register(String plantId, String deviceKey) {
    if (plantId == null || plantId.isBlank() || deviceKey == null || deviceKey.isBlank()) {
      log.debug("Ignoring invalid mapping (plantId or deviceKey is null/blank)");
      return;
    }
    String previous = plantToDevice.put(plantId, deviceKey);
    if (previous == null || !previous.equals(deviceKey)) {
      log.debug("Mapped plantId={} → deviceKey={}", plantId, deviceKey);
    }
  }

  public Optional<String> resolveDeviceKey(String plantId) {
    return Optional.ofNullable(plantToDevice.get(plantId));
  }
}

package com.smartgarden.integration.cache;

import com.smartgarden.integration.dto.device.DeviceStatusDto;
import com.smartgarden.integration.dto.device.IoTDeviceDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registro em memória de devices IoT conhecidos.
 * Devices são registrados automaticamente ao chegar a primeira mensagem MQTT.
 */
@Slf4j
@Component
public class DeviceCache {

  private final ConcurrentHashMap<String, IoTDeviceDto> store = new ConcurrentHashMap<>();

  public void registerDevice(String deviceId, IoTDeviceDto device) {
    store.put(deviceId, device);
    log.debug("Device registered: {}", deviceId);
  }

  public void updateStatus(String deviceId, DeviceStatusDto status) {
    store.compute(deviceId, (id, existing) -> {
      if (existing == null) {
        return IoTDeviceDto.builder()
            .id(deviceId)
            .name(deviceId)
            .ip(status.getIp())
            .description("Auto-discovered via MQTT")
            .status(status)
            .build();
      }
      existing.setStatus(status);
      if (status.getIp() != null) {
        existing.setIp(status.getIp());
      }
      return existing;
    });
  }

  public Optional<IoTDeviceDto> getDevice(String deviceId) {
    return Optional.ofNullable(store.get(deviceId));
  }

  public List<IoTDeviceDto> getAllDevices() {
    return List.copyOf(store.values());
  }

  public boolean exists(String deviceId) {
    return store.containsKey(deviceId);
  }

  public void clear() {
    store.clear();
  }
}

package com.smartgarden.integration.service;

import com.smartgarden.integration.cache.DeviceCache;
import com.smartgarden.integration.cache.ReadingCache;
import com.smartgarden.integration.dto.device.DeviceCommandDto;
import com.smartgarden.integration.dto.device.DeviceStatusDto;
import com.smartgarden.integration.dto.device.IoTDeviceDto;
import com.smartgarden.integration.dto.device.IrrigationReadingDto;
import com.smartgarden.integration.exception.ResourceNotFoundException;
import com.smartgarden.integration.mqtt.publisher.CommandMqttPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceService {

  private final DeviceCache deviceCache;
  private final ReadingCache readingCache;
  private final CommandMqttPublisher commandMqttPublisher;
  private final PlantService plantService;

  public List<IoTDeviceDto> listDevices() {
    return deviceCache.getAllDevices();
  }

  public IoTDeviceDto getDevice(String deviceId) {
    return deviceCache.getDevice(deviceId)
        .orElseThrow(() -> new ResourceNotFoundException("Device not found: " + deviceId));
  }

  public DeviceStatusDto getDeviceStatus(String deviceId) {
    return deviceCache.getDevice(deviceId)
        .map(d -> d.getStatus() != null ? d.getStatus()
            : DeviceStatusDto.builder().deviceId(deviceId).online(false).build())
        .orElseThrow(() -> new ResourceNotFoundException("Device not found: " + deviceId));
  }

  // public List<IrrigationReadingDto> getReadings(String deviceId) {
  // return readingCache.getReadingsForDevice(deviceId);
  // }
  public List<IrrigationReadingDto> getReadings(String deviceId) {
    // Tenta cache em memória primeiro
    List<IrrigationReadingDto> cached = readingCache.getReadingsForDevice(deviceId);
    if (!cached.isEmpty()) {
      return cached;
    }

    // Cache vazio (restart ou device ainda não enviou leitura nesta sessão).
    // Busca histórico no plant-management-service via AMQP.
    log.info("ReadingCache empty for deviceId={} — fetching from domain via AMQP", deviceId);
    try {
      return plantService.getReadingsByDeviceFromDomain(deviceId);
    } catch (Exception e) {
      log.warn("AMQP fallback failed for deviceId={}: {}", deviceId, e.getMessage());
      return List.of();
    }
  }

  public void sendCommand(String deviceId, DeviceCommandDto command) {
    // DeviceCache é populado via MQTT (status ou primeira leitura).
    // Se o device não estiver aqui, está genuinamente offline ou nunca se conectou.
    if (!deviceCache.exists(deviceId)) {
      throw new ResourceNotFoundException("Device not found: " + deviceId);
    }
    log.info("Sending command '{}' to device {}", command.getType(), deviceId);
    Map<String, Object> payload = Map.of(
        "type", command.getType(),
        "params", command.getParams() != null ? command.getParams() : Map.of());
    commandMqttPublisher.publishCommand(deviceId, payload);
  }

  public void updateStatus(String deviceId, DeviceStatusDto status) {
    deviceCache.updateStatus(deviceId, status);
  }

  public void registerIfAbsent(String deviceId, String ip) {
    if (deviceCache.exists(deviceId))
      return;

    IoTDeviceDto device = IoTDeviceDto.builder()
        .id(deviceId)
        .name(deviceId)
        .ip(ip)
        .description("Auto-discovered via MQTT")
        .status(DeviceStatusDto.builder()
            .deviceId(deviceId)
            .ip(ip)
            .online(true)
            .lastSeen(Instant.now())
            .build())
        .build();
    deviceCache.registerDevice(deviceId, device);
    log.info("Auto-registered new device: {}", deviceId);
  }
}

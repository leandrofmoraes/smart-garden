package com.smartgarden.integration.cache;

import com.smartgarden.integration.dto.device.IrrigationReadingDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache em memória das últimas leituras IoT por device.
 * Mantém no máximo MAX_READINGS_PER_DEVICE leituras por device (FIFO).
 */
@Slf4j
@Component
public class ReadingCache {

  private static final int MAX_READINGS_PER_DEVICE = 50;

  private final ConcurrentHashMap<String, List<IrrigationReadingDto>> store = new ConcurrentHashMap<>();

  public void put(String deviceId, IrrigationReadingDto reading) {
    store.compute(deviceId, (id, existing) -> {
      List<IrrigationReadingDto> list = existing != null ? existing : new ArrayList<>();
      list.add(reading);
      if (list.size() > MAX_READINGS_PER_DEVICE) {
        list.remove(0);
      }
      return list;
    });
    log.debug("Reading cached for device {}", deviceId);
  }

  public List<IrrigationReadingDto> getReadingsForDevice(String deviceId) {
    return List.copyOf(store.getOrDefault(deviceId, List.of()));
  }

  public List<IrrigationReadingDto> getLatestPerDevice() {
    List<IrrigationReadingDto> latest = new ArrayList<>();
    store.forEach((deviceId, readings) -> {
      if (!readings.isEmpty()) {
        latest.add(readings.get(readings.size() - 1));
      }
    });
    return latest;
  }

  public void clear() {
    store.clear();
  }
}

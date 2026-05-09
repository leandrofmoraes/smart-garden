package com.smartgarden.integration.service;

import com.smartgarden.integration.cache.AlertCache;
import com.smartgarden.integration.cache.DeviceCache;
import com.smartgarden.integration.dto.dashboard.DashboardPlantCardDto;
import com.smartgarden.integration.dto.dashboard.DashboardReadingCardDto;
import com.smartgarden.integration.dto.dashboard.DashboardResponseDto;
import com.smartgarden.integration.dto.device.DeviceStatusDto;
import com.smartgarden.integration.dto.device.IrrigationReadingDto;
import com.smartgarden.integration.dto.messaging.AmqpPlantResponseDto;
import com.smartgarden.integration.dto.plant.PlantResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Agrega dados de plantas, leituras e devices para o dashboard.
 *
 * <p>
 * Mantém um cache local de plantas ({@code plantCache}) para evitar bloqueio
 * de até 5s (timeout AMQP) a cada requisição {@code GET /api/dashboard}.
 * O cache é atualizado a cada chamada bem-sucedida e serve dados stale em caso
 * de indisponibilidade do plant-management-service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

  private final PlantService plantService;
  private final DeviceCache deviceCache;
  private final ReadingService readingService;
  private final AlertCache alertCache;

  /**
   * Cache local de plantas — evita round-trip AMQP síncrono a cada dashboard
   * request.
   */
  private final CopyOnWriteArrayList<PlantResponseDto> plantCache = new CopyOnWriteArrayList<>();

  public DashboardResponseDto getDashboard() {
    refreshPlantCacheQuietly();

    List<DashboardPlantCardDto> plantCards = plantCache.stream()
        .map(p -> DashboardPlantCardDto.builder()
            .id(p.getId())
            .name(p.getName())
            .scientificName(p.getScientificName())
            .imageUrl(p.getImageUrl())
            .care(p.getCare())
            .build())
        .toList();

    List<IrrigationReadingDto> latestReadings = readingService.getLatestPerDevice();

    List<DashboardReadingCardDto> readingCards = latestReadings.stream()
        .map(r -> DashboardReadingCardDto.builder()
            .deviceId(r.getDeviceId())
            .deviceName(resolveDeviceName(r.getDeviceId()))
            .humidity(r.getHumidity())
            .regando(r.getRegando())
            .espRssi(r.getEspRssi())
            .espIp(r.getEspIp())
            .lastUpdated(r.getTimestamp())
            .build())
        .toList();

    long online = deviceCache.getAllDevices().stream()
        .filter(d -> d.getStatus() != null && Boolean.TRUE.equals(d.getStatus().getOnline()))
        .count();

    return DashboardResponseDto.builder()
        .plants(plantCards)
        .latestReadings(readingCards)
        .onlineDevices((int) online)
        .totalDevices(deviceCache.getAllDevices().size())
        .generatedAt(Instant.now())
        .build();
  }

  public List<DeviceStatusDto> getAllDeviceStatuses() {
    return deviceCache.getAllDevices().stream()
        .map(d -> d.getStatus() != null ? d.getStatus()
            : DeviceStatusDto.builder().deviceId(d.getId()).online(false).build())
        .toList();
  }

  /**
   * Retorna e consome os alertas pendentes (leitura destrutiva).
   *
   * @see AlertCache#consumeAlerts()
   */
  public List<AmqpPlantResponseDto> getPendingAlerts() {
    return alertCache.consumeAlerts();
  }

  private void refreshPlantCacheQuietly() {
    try {
      List<PlantResponseDto> fresh = plantService.listPlants();
      plantCache.clear();
      plantCache.addAll(fresh);
      log.debug("Plant cache refreshed, {} plant(s) loaded", fresh.size());
    } catch (Exception e) {
      log.warn("Could not refresh plant cache — serving stale data. Reason: {}",
          e.getMessage());
    }
  }

  private String resolveDeviceName(String deviceId) {
    return deviceCache.getDevice(deviceId)
        .map(d -> d.getName() != null ? d.getName() : deviceId)
        .orElse(deviceId);
  }
}

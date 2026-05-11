package com.smartgarden.integration.service;

import com.smartgarden.integration.cache.AlertCache;
import com.smartgarden.integration.cache.DeviceCache;
import com.smartgarden.integration.dto.dashboard.DashboardResponseDto;
import com.smartgarden.integration.dto.device.DeviceStatusDto;
import com.smartgarden.integration.dto.device.IoTDeviceDto;
import com.smartgarden.integration.dto.device.IrrigationReadingDto;
import com.smartgarden.integration.dto.messaging.AmqpPlantResponseDto;
import com.smartgarden.integration.dto.plant.PlantResponseDto;
import com.smartgarden.integration.exception.IntegrationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

  @Mock
  PlantService plantService;
  @Mock
  DeviceCache deviceCache;
  @Mock
  ReadingService readingService;
  @Mock
  AlertCache alertCache;

  @InjectMocks
  DashboardService dashboardService;

  @Test
  void getDashboard_shouldAggregateAllData() {
    when(plantService.listPlants()).thenReturn(List.of(
        PlantResponseDto.builder().id("1").name("Samambaia").build()));
    when(readingService.getLatestPerDevice()).thenReturn(List.of(
        IrrigationReadingDto.builder()
            .deviceId("esp-01").humidity(65.0)
            .regando(false).timestamp(Instant.now()).build()));
    when(deviceCache.getAllDevices()).thenReturn(List.of(
        deviceWithStatus("esp-01", true),
        deviceWithStatus("esp-02", false)));
    when(deviceCache.getDevice("esp-01")).thenReturn(
        java.util.Optional.of(deviceWithStatus("esp-01", true)));

    DashboardResponseDto result = dashboardService.getDashboard();

    assertThat(result.getPlants()).hasSize(1);
    assertThat(result.getLatestReadings()).hasSize(1);
    assertThat(result.getOnlineDevices()).isEqualTo(1);
    assertThat(result.getTotalDevices()).isEqualTo(2);
    assertThat(result.getGeneratedAt()).isNotNull();
  }

  @Test
  void getDashboard_whenPlantServiceFails_shouldUseStaleCache() {
    when(plantService.listPlants())
        .thenReturn(List.of(PlantResponseDto.builder().id("1").name("Cached").build()))
        .thenThrow(new IntegrationException("timeout"));
    when(readingService.getLatestPerDevice()).thenReturn(List.of());
    when(deviceCache.getAllDevices()).thenReturn(List.of());

    // Primeira chamada: popula cache
    dashboardService.getDashboard();
    // Segunda chamada: AMQP falha, usa stale cache
    DashboardResponseDto result = dashboardService.getDashboard();

    assertThat(result.getPlants()).hasSize(1);
    assertThat(result.getPlants().get(0).getName()).isEqualTo("Cached");
  }

  @Test
  void getDashboard_whenPlantServiceFailsAndCacheEmpty_shouldReturnEmptyPlants() {
    when(plantService.listPlants()).thenThrow(new IntegrationException("AMQP down"));
    when(readingService.getLatestPerDevice()).thenReturn(List.of());
    when(deviceCache.getAllDevices()).thenReturn(List.of());

    DashboardResponseDto result = dashboardService.getDashboard();

    assertThat(result.getPlants()).isEmpty();
  }

  @Test
  void getPendingAlerts_shouldDrainAlertCache() {
    when(alertCache.consumeAlerts()).thenReturn(List.of(
        AmqpPlantResponseDto.builder().correlationId("a1").success(false).build()));

    List<AmqpPlantResponseDto> alerts = dashboardService.getPendingAlerts();
    assertThat(alerts).hasSize(1);
    assertThat(alerts.get(0).getCorrelationId()).isEqualTo("a1");
  }

  @Test
  void getAllDeviceStatuses_shouldReturnOfflineForMissingStatus() {
    IoTDeviceDto deviceWithoutStatus = IoTDeviceDto.builder().id("esp-no-status").build();
    when(deviceCache.getAllDevices()).thenReturn(List.of(deviceWithoutStatus));

    List<DeviceStatusDto> statuses = dashboardService.getAllDeviceStatuses();

    assertThat(statuses).hasSize(1);
    assertThat(statuses.get(0).getOnline()).isNull();
    assertThat(statuses.get(0).getDeviceId()).isEqualTo("esp-no-status");
  }

  private IoTDeviceDto deviceWithStatus(String id, boolean online) {
    return IoTDeviceDto.builder()
        .id(id)
        .name("Device " + id)
        .status(DeviceStatusDto.builder()
            .deviceId(id).online(online).lastSeen(Instant.now()).build())
        .build();
  }
}

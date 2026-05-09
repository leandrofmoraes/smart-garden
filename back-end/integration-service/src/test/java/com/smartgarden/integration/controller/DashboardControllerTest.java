package com.smartgarden.integration.controller;

import com.smartgarden.integration.dto.dashboard.DashboardResponseDto;
import com.smartgarden.integration.dto.device.DeviceStatusDto;
import com.smartgarden.integration.dto.messaging.AmqpPlantResponseDto;
import com.smartgarden.integration.service.DashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DashboardController.class)
class DashboardControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private DashboardService dashboardService;

  @Test
  void getDashboard_shouldReturn200() throws Exception {
    when(dashboardService.getDashboard()).thenReturn(DashboardResponseDto.builder()
        .plants(List.of())
        .latestReadings(List.of())
        .onlineDevices(2)
        .totalDevices(3)
        .generatedAt(Instant.now())
        .build());

    mockMvc.perform(get("/api/dashboard"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.onlineDevices").value(2))
        .andExpect(jsonPath("$.totalDevices").value(3));
  }

  @Test
  void getStatus_shouldReturn200() throws Exception {
    when(dashboardService.getAllDeviceStatuses()).thenReturn(List.of(
        DeviceStatusDto.builder().deviceId("esp-01").online(true).build()));

    mockMvc.perform(get("/api/dashboard/status"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].deviceId").value("esp-01"))
        .andExpect(jsonPath("$[0].online").value(true));
  }

  @Test
  void getAlerts_shouldReturn200() throws Exception {
    when(dashboardService.getPendingAlerts()).thenReturn(List.of(
        AmqpPlantResponseDto.builder().correlationId("c1").success(false).build()));

    mockMvc.perform(get("/api/dashboard/alerts"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].correlationId").value("c1"));
  }

  @Test
  void getDashboard_whenServiceFails_shouldReturn502() throws Exception {
    when(dashboardService.getDashboard())
        .thenThrow(new com.smartgarden.integration.exception.IntegrationException("AMQP down"));

    mockMvc.perform(get("/api/dashboard"))
        .andExpect(status().isBadGateway());
  }
}

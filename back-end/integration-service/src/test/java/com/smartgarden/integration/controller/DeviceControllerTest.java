package com.smartgarden.integration.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartgarden.integration.dto.device.DeviceCommandDto;
import com.smartgarden.integration.dto.device.DeviceStatusDto;
import com.smartgarden.integration.dto.device.IoTDeviceDto;
import com.smartgarden.integration.exception.ResourceNotFoundException;
import com.smartgarden.integration.service.DeviceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DeviceController.class)
class DeviceControllerTest {

  @Autowired
  MockMvc mockMvc;
  @Autowired
  ObjectMapper objectMapper;
  @MockitoBean
  DeviceService deviceService;

  @Test
  void listDevices_shouldReturn200() throws Exception {
    when(deviceService.listDevices()).thenReturn(List.of(
        IoTDeviceDto.builder().id("esp-01").name("Sensor Jardim").build()));

    mockMvc.perform(get("/api/devices"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value("esp-01"));
  }

  @Test
  void getDevice_shouldReturn200() throws Exception {
    when(deviceService.getDevice("esp-01")).thenReturn(
        IoTDeviceDto.builder().id("esp-01").name("Sensor Jardim").ip("192.168.1.10").build());

    mockMvc.perform(get("/api/devices/esp-01"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ip").value("192.168.1.10"));
  }

  @Test
  void getDevice_notFound_shouldReturn404() throws Exception {
    when(deviceService.getDevice("unknown"))
        .thenThrow(new ResourceNotFoundException("Device not found: unknown"));

    mockMvc.perform(get("/api/devices/unknown"))
        .andExpect(status().isNotFound());
  }

  @Test
  void getDeviceStatus_shouldReturn200() throws Exception {
    when(deviceService.getDeviceStatus("esp-01")).thenReturn(
        DeviceStatusDto.builder().deviceId("esp-01").online(true).ip("192.168.1.10").build());

    mockMvc.perform(get("/api/devices/esp-01/status"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.online").value(true));
  }

  @Test
  void sendCommand_shouldReturn202() throws Exception {
    doNothing().when(deviceService).sendCommand(eq("esp-01"), any());

    DeviceCommandDto cmd = DeviceCommandDto.builder()
        .type("IRRIGATE")
        .params(Map.of("durationSeconds", 30))
        .build();

    mockMvc.perform(post("/api/devices/esp-01/commands")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(cmd)))
        .andExpect(status().isAccepted());
  }

  @Test
  void sendCommand_withBlankType_shouldReturn422() throws Exception {
    DeviceCommandDto cmd = DeviceCommandDto.builder().type("").build();

    mockMvc.perform(post("/api/devices/esp-01/commands")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(cmd)))
        .andExpect(status().isUnprocessableContent());
  }

  @Test
  void sendCommand_deviceNotFound_shouldReturn404() throws Exception {
    doThrow(new ResourceNotFoundException("Device not found: ghost"))
        .when(deviceService).sendCommand(eq("ghost"), any());

    DeviceCommandDto cmd = DeviceCommandDto.builder().type("IRRIGATE").build();

    mockMvc.perform(post("/api/devices/ghost/commands")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(cmd)))
        .andExpect(status().isNotFound());
  }
}

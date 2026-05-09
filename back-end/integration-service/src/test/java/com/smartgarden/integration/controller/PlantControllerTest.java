package com.smartgarden.integration.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartgarden.integration.dto.plant.PlantRequestDto;
import com.smartgarden.integration.dto.plant.PlantResponseDto;
import com.smartgarden.integration.exception.IntegrationException;
import com.smartgarden.integration.service.PlantService;
import com.smartgarden.integration.service.ReadingService;

@WebMvcTest(PlantController.class)
class PlantControllerTest {

  @Autowired
  MockMvc mockMvc;
  @Autowired
  ObjectMapper objectMapper;

  @MockitoBean
  PlantService plantService;
  @MockitoBean
  ReadingService readingService;

  @Test
  void listPlants_shouldReturn200() throws Exception {
    when(plantService.listPlants()).thenReturn(List.of(
        PlantResponseDto.builder().id("1").name("Samambaia").build()));

    mockMvc.perform(get("/api/plants"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("Samambaia"));
  }

  @Test
  void getPlant_shouldReturn200() throws Exception {
    when(plantService.getPlant("1")).thenReturn(
        PlantResponseDto.builder().id("1").name("Orquídea").build());

    mockMvc.perform(get("/api/plants/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Orquídea"));
  }

  @Test
  void createPlant_shouldReturn201() throws Exception {
    PlantRequestDto request = PlantRequestDto.builder()
        .name("Samambaia").scientificName("Nephrolepis exaltata").build();

    when(plantService.createPlant(any())).thenReturn(
        PlantResponseDto.builder().id("99").name("Samambaia").build());

    mockMvc.perform(post("/api/plants")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value("99"));
  }

  @Test
  void createPlant_withBlankName_shouldReturn422() throws Exception {
    PlantRequestDto request = PlantRequestDto.builder().name("").build();

    mockMvc.perform(post("/api/plants")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnprocessableContent())
        .andExpect(jsonPath("$.fieldErrors.name").exists());
  }

  @Test
  void updatePlant_shouldReturn200() throws Exception {
    PlantRequestDto request = PlantRequestDto.builder().name("Atualizada").build();

    when(plantService.updatePlant(eq("1"), any())).thenReturn(
        PlantResponseDto.builder().id("1").name("Atualizada").build());

    mockMvc.perform(put("/api/plants/1")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Atualizada"));
  }

  @Test
  void deletePlant_shouldReturn204() throws Exception {
    doNothing().when(plantService).deletePlant("1");

    mockMvc.perform(delete("/api/plants/1"))
        .andExpect(status().isNoContent());
  }

  @Test
  void getPlant_whenTimeout_shouldReturn502() throws Exception {
    when(plantService.getPlant("1"))
        .thenThrow(new IntegrationException("Timeout"));

    mockMvc.perform(get("/api/plants/1"))
        .andExpect(status().isBadGateway());
  }
}

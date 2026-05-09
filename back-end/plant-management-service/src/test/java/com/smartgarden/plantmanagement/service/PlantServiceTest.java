package com.smartgarden.plantmanagement.service;

import com.smartgarden.plantmanagement.client.PlantBookClient;
import com.smartgarden.plantmanagement.dto.plant.PlantRequestDto;
import com.smartgarden.plantmanagement.dto.plant.PlantResponseDto;
import com.smartgarden.plantmanagement.exception.PlantNotFoundException;
import com.smartgarden.plantmanagement.mapper.PlantMapper;
import com.smartgarden.plantmanagement.model.PlantModel;
import com.smartgarden.plantmanagement.repository.PlantRepository;
import com.smartgarden.plantmanagement.util.PlantNameTranslator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlantServiceTest {

  @Mock
  PlantRepository plantRepository;
  @Mock
  PlantMapper plantMapper;
  @Mock
  PlantNameTranslator nameTranslator;
  @Mock
  PlantBookClient plantBookClient;

  @InjectMocks
  PlantService plantService;

  @Test
  void listAll_shouldReturnMappedPlants() {
    PlantModel plant = PlantModel.builder().id(UUID.randomUUID()).name("Samambaia").build();
    PlantResponseDto dto = PlantResponseDto.builder()
        .id(plant.getId().toString()).name("Samambaia").build();

    when(plantRepository.findAllWithCare()).thenReturn(List.of(plant));
    when(plantMapper.toResponseDtoList(List.of(plant))).thenReturn(List.of(dto));

    List<PlantResponseDto> result = plantService.listAll();
    assertThat(result).hasSize(1).first().extracting(PlantResponseDto::getName)
        .isEqualTo("Samambaia");
  }

  @Test
  void getById_shouldThrowWhenNotFound() {
    when(plantRepository.findByIdWithCare(any())).thenReturn(Optional.empty());
    assertThatThrownBy(() -> plantService.getById(UUID.randomUUID().toString()))
        .isInstanceOf(PlantNotFoundException.class);
  }

  @Test
  void create_shouldPersistAndReturnDto() {
    UUID savedId = UUID.randomUUID();
    PlantModel savedPlant = PlantModel.builder().id(savedId).name("Orquídea").build();
    PlantResponseDto responseDto = PlantResponseDto.builder()
        .id(savedId.toString()).name("Orquídea").build();

    when(nameTranslator.translate(any())).thenReturn("phalaenopsis amabilis");
    when(plantBookClient.searchAndGetDetail(any())).thenReturn(Optional.empty());
    // dois saves: 1o persiste base, 2o com care (não chamado pois PlantBook e DTO
    // care são null)
    when(plantRepository.save(any())).thenReturn(savedPlant);
    when(plantMapper.toResponseDto(savedPlant)).thenReturn(responseDto);

    PlantResponseDto result = plantService.create(
        PlantRequestDto.builder().name("Orquídea").build());

    assertThat(result.getName()).isEqualTo("Orquídea");
    // save chamado pelo menos 1x (base), pode ser chamado 2x se houver care
    verify(plantRepository, atLeastOnce()).save(any());
  }

  @Test
  void create_shouldContinueEvenWhenPlantBookFails() {
    PlantModel saved = PlantModel.builder().id(UUID.randomUUID()).name("Samambaia").build();

    when(nameTranslator.translate(any())).thenReturn("nephrolepis exaltata");
    when(plantBookClient.searchAndGetDetail(any()))
        .thenThrow(new RuntimeException("API unreachable"));
    when(plantRepository.save(any())).thenReturn(saved);
    when(plantMapper.toResponseDto(any())).thenReturn(
        PlantResponseDto.builder().name("Samambaia").build());

    assertThatCode(() -> plantService.create(
        PlantRequestDto.builder().name("Samambaia").build()))
        .doesNotThrowAnyException();
  }

  @Test
  void delete_shouldThrowWhenNotFound() {
    when(plantRepository.findByIdWithCare(any())).thenReturn(Optional.empty());
    assertThatThrownBy(() -> plantService.delete(UUID.randomUUID().toString()))
        .isInstanceOf(PlantNotFoundException.class);
  }

  @Test
  void delete_shouldCallRepositoryDelete() {
    PlantModel plant = PlantModel.builder().id(UUID.randomUUID()).name("Rosa").build();
    when(plantRepository.findByIdWithCare(plant.getId())).thenReturn(Optional.of(plant));

    plantService.delete(plant.getId().toString());

    verify(plantRepository).delete(plant);
  }
}

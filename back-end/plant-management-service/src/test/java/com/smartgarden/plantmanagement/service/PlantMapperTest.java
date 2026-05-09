package com.smartgarden.plantmanagement.service;

import com.smartgarden.plantmanagement.dto.plant.*;
import com.smartgarden.plantmanagement.mapper.PlantMapper;
import com.smartgarden.plantmanagement.model.PlantCareModel;
import com.smartgarden.plantmanagement.model.PlantModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PlantMapperTest {

  private PlantMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = new PlantMapper();
  }

  @Test
  void toResponseDto_shouldMapAllFields() {
    UUID id = UUID.randomUUID();
    PlantModel plant = PlantModel.builder()
        .id(id).name("Samambaia").scientificName("Nephrolepis exaltata")
        .imageUrl("http://img.jpg").build();

    PlantCareModel care = PlantCareModel.builder()
        .plant(plant)
        .lightLuxMin(2000.0).lightLuxMax(10000.0)
        .tempMin(16.0).tempMax(26.0)
        .envHumidityMin(50.0).envHumidityMax(80.0)
        .soilMoistureMin(40.0).soilMoistureMax(70.0)
        .build();
    plant.setCare(care);

    PlantResponseDto dto = mapper.toResponseDto(plant);

    assertThat(dto.getId()).isEqualTo(id.toString());
    assertThat(dto.getName()).isEqualTo("Samambaia");
    assertThat(dto.getCare()).isNotNull();
    assertThat(dto.getCare().getSoilMoisture().getMin()).isEqualTo(40.0);
    assertThat(dto.getCare().getLightLux().getMax()).isEqualTo(10000.0);
  }

  @Test
  void toResponseDto_shouldHandleNullCare() {
    PlantModel plant = PlantModel.builder()
        .id(UUID.randomUUID()).name("Cactus").build();
    PlantResponseDto dto = mapper.toResponseDto(plant);
    assertThat(dto.getCare()).isNull();
  }

  @Test
  void toCareModel_shouldMapCorrectly() {
    PlantModel plant = PlantModel.builder().id(UUID.randomUUID()).name("Test").build();
    PlantCareDto careDto = PlantCareDto.builder()
        .soilMoisture(new CareRangeDto(40.0, 70.0))
        .temperature(new CareRangeDto(18.0, 28.0))
        .build();

    PlantCareModel model = mapper.toCareModel(careDto, plant);

    assertThat(model.getSoilMoistureMin()).isEqualTo(40.0);
    assertThat(model.getSoilMoistureMax()).isEqualTo(70.0);
    assertThat(model.getTempMin()).isEqualTo(18.0);
    assertThat(model.getLightLuxMin()).isNull();
  }

  @Test
  void toCareModel_shouldReturnNullForNullDto() {
    PlantModel plant = PlantModel.builder().id(UUID.randomUUID()).build();
    assertThat(mapper.toCareModel(null, plant)).isNull();
  }
}

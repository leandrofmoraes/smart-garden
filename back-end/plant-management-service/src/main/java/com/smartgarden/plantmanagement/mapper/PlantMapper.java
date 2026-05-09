package com.smartgarden.plantmanagement.mapper;

import com.smartgarden.plantmanagement.dto.plant.*;
import com.smartgarden.plantmanagement.model.PlantCareModel;
import com.smartgarden.plantmanagement.model.PlantModel;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Converte entre modelos JPA e DTOs de planta.
 * Isolado para facilitar testes unitários.
 */
@Component
public class PlantMapper {

  public PlantResponseDto toResponseDto(PlantModel plant) {
    return PlantResponseDto.builder()
        .id(plant.getId() != null ? plant.getId().toString() : null)
        .name(plant.getName())
        .scientificName(plant.getScientificName())
        .imageUrl(plant.getImageUrl())
        .care(toCareDto(plant.getCare()))
        .build();
  }

  public List<PlantResponseDto> toResponseDtoList(List<PlantModel> plants) {
    return plants.stream().map(this::toResponseDto).toList();
  }

  public PlantCareDto toCareDto(PlantCareModel care) {
    if (care == null)
      return null;
    return PlantCareDto.builder()
        .lightLux(new CareRangeDto(care.getLightLuxMin(), care.getLightLuxMax()))
        .temperature(new CareRangeDto(care.getTempMin(), care.getTempMax()))
        .envHumidity(new CareRangeDto(care.getEnvHumidityMin(), care.getEnvHumidityMax()))
        .soilMoisture(new CareRangeDto(care.getSoilMoistureMin(), care.getSoilMoistureMax()))
        .build();
  }

  public PlantCareModel toCareModel(PlantCareDto dto, PlantModel plant) {
    if (dto == null)
      return null;
    return PlantCareModel.builder()
        .plant(plant)
        .lightLuxMin(dto.getLightLux() != null ? dto.getLightLux().getMin() : null)
        .lightLuxMax(dto.getLightLux() != null ? dto.getLightLux().getMax() : null)
        .tempMin(dto.getTemperature() != null ? dto.getTemperature().getMin() : null)
        .tempMax(dto.getTemperature() != null ? dto.getTemperature().getMax() : null)
        .envHumidityMin(dto.getEnvHumidity() != null ? dto.getEnvHumidity().getMin() : null)
        .envHumidityMax(dto.getEnvHumidity() != null ? dto.getEnvHumidity().getMax() : null)
        .soilMoistureMin(dto.getSoilMoisture() != null ? dto.getSoilMoisture().getMin() : null)
        .soilMoistureMax(dto.getSoilMoisture() != null ? dto.getSoilMoisture().getMax() : null)
        .build();
  }
}

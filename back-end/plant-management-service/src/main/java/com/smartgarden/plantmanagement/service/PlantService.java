package com.smartgarden.plantmanagement.service;

import com.smartgarden.plantmanagement.client.PlantBookClient;
import com.smartgarden.plantmanagement.client.dto.PlantBookDetailResponse;
import com.smartgarden.plantmanagement.dto.plant.*;
import com.smartgarden.plantmanagement.exception.PlantNotFoundException;
import com.smartgarden.plantmanagement.mapper.PlantMapper;
import com.smartgarden.plantmanagement.model.PlantCareModel;
import com.smartgarden.plantmanagement.model.PlantModel;
import com.smartgarden.plantmanagement.repository.DeviceRepository;
import com.smartgarden.plantmanagement.repository.PlantRepository;
import com.smartgarden.plantmanagement.util.PlantNameTranslator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlantService {

  private final PlantRepository plantRepository;
  private final DeviceRepository deviceRepository;
  private final PlantMapper plantMapper;
  private final PlantNameTranslator nameTranslator;
  private final PlantBookClient plantBookClient;
  private final AlertService alertService;

  @Transactional(readOnly = true)
  public List<PlantResponseDto> listAll() {
    return plantMapper.toResponseDtoList(plantRepository.findAllWithCare());
  }

  @Transactional(readOnly = true)
  public PlantResponseDto getById(String id) {
    return plantMapper.toResponseDto(findById(id));
  }

  /**
   * Cria uma nova planta.
   *
   * <p>
   * Fluxo:
   * <ol>
   * <li>Persiste o PlantModel sem care (para obter o id)</li>
   * <li>Tenta enriquecer via Open PlantBook</li>
   * <li>Usa care do DTO como fallback</li>
   * <li>Salva novamente com care (cascade ALL persiste o PlantCareModel)</li>
   * </ol>
   */
  @Transactional
  public PlantResponseDto create(PlantRequestDto dto) {
    log.info("Creating plant: name={}", dto.getName());

    // 1. Persiste a planta base para garantir o id antes do care
    PlantModel plant = PlantModel.builder()
        .name(dto.getName())
        .scientificName(dto.getScientificName())
        .imageUrl(dto.getImageUrl())
        .build();
    plant = plantRepository.save(plant);

    // 2. Tenta enriquecer com Open PlantBook
    PlantCareModel care = enrichFromPlantBook(dto, plant);

    // 3. Fallback para care do DTO se PlantBook não retornar dados
    if (care == null && dto.getCare() != null) {
      care = plantMapper.toCareModel(dto.getCare(), plant);
    }

    if (care != null) {
      plant.setCare(care);
      plant = plantRepository.save(plant); // cascade ALL persiste o care
    }

    linkDeviceToPlant(plant, dto.getDeviceKey());

    log.info("Plant created: id={}, name={}", plant.getId(), plant.getName());
    return plantMapper.toResponseDto(plant);
  }

  @Transactional
  public PlantResponseDto update(String id, PlantRequestDto dto) {
    PlantModel plant = findById(id);

    plant.setName(dto.getName());
    if (dto.getScientificName() != null)
      plant.setScientificName(dto.getScientificName());
    if (dto.getImageUrl() != null)
      plant.setImageUrl(dto.getImageUrl());

    if (dto.getCare() != null) {
      if (plant.getCare() == null) {
        plant.setCare(plantMapper.toCareModel(dto.getCare(), plant));
      } else {
        updateCareFields(plant.getCare(), dto.getCare());
      }
    }

    PlantModel saved = plantRepository.save(plant);
    log.info("Plant updated: id={}", saved.getId());
    return plantMapper.toResponseDto(saved);
  }

  // @Transactional
  // public void delete(String id) {
  // PlantModel plant = findById(id);
  // plantRepository.delete(plant);
  // log.info("Plant deleted: id={}", id);
  // }

  @Transactional
  public void delete(String id) {
    PlantModel plant = findById(id);
    // Remove alertas associados antes de deletar a planta
    alertService.deleteAlertsByPlantId(plant.getId());
    deviceRepository.unlinkPlantFromDevices(plant.getId());
    plantRepository.delete(plant);
    log.info("Plant deleted: id={}", id);
  }
  /* ------------------------------------------------------------------ */

  private PlantModel findById(String id) {
    return plantRepository.findByIdWithCare(UUID.fromString(id))
        .orElseThrow(() -> new PlantNotFoundException(id));
  }

  private void linkDeviceToPlant(PlantModel plant, String deviceKey) {
    if (deviceKey == null || deviceKey.isBlank()) {
      return;
    }

    deviceRepository.findByDeviceKey(deviceKey).ifPresentOrElse(device -> {
      device.setPlant(plant);
      deviceRepository.save(device);
      log.info("Device '{}' linked to plant id={}", deviceKey, plant.getId());
    }, () -> {
      log.warn("DeviceKey '{}' not found – plant created without device link", deviceKey);
    });
  }

  /**
   * Consulta a Open PlantBook e constrói um {@link PlantCareModel} com os dados
   * retornados.
   * Retorna {@code null} se a busca falhar ou não retornar resultado —
   * o cadastro prossegue mesmo sem enriquecimento externo.
   */
  private PlantCareModel enrichFromPlantBook(PlantRequestDto dto, PlantModel plant) {
    String searchName = dto.getScientificName() != null ? dto.getScientificName() : dto.getName();
    String alias = nameTranslator.translate(searchName);

    try {
      Optional<PlantBookDetailResponse> detailOpt = plantBookClient.searchAndGetDetail(alias);

      if (detailOpt.isEmpty()) {
        log.warn("PlantBook returned no detail for alias='{}' — skipping enrichment", alias);
        return null;
      }

      PlantBookDetailResponse detail = detailOpt.get();
      plant.setPlantbookPid(detail.getPid());

      // Enriquece campos da planta se ainda não foram fornecidos pelo usuário
      if (plant.getImageUrl() == null && detail.getImageUrl() != null) {
        plant.setImageUrl(detail.getImageUrl());
      }
      if (plant.getScientificName() == null && detail.getAlias() != null) {
        plant.setScientificName(detail.getAlias());
      }

      log.info("PlantBook enrichment OK [pid={}, alias='{}']", detail.getPid(), alias);

      return PlantCareModel.builder()
          .plant(plant)
          .lightLuxMin(toDouble(detail.getMinLightLux()))
          .lightLuxMax(toDouble(detail.getMaxLightLux()))
          .tempMin(detail.getMinTemp())
          .tempMax(detail.getMaxTemp())
          .envHumidityMin(detail.getMinEnvHumid())
          .envHumidityMax(detail.getMaxEnvHumid())
          .soilMoistureMin(detail.getMinSoilMoist())
          .soilMoistureMax(detail.getMaxSoilMoist())
          .build();

    } catch (Exception e) {
      log.warn("PlantBook enrichment failed for alias='{}': {} — proceeding without enrichment",
          alias, e.getMessage());
      return null;
    }
  }

  private void updateCareFields(PlantCareModel care, PlantCareDto dto) {
    if (dto.getLightLux() != null) {
      care.setLightLuxMin(dto.getLightLux().getMin());
      care.setLightLuxMax(dto.getLightLux().getMax());
    }
    if (dto.getTemperature() != null) {
      care.setTempMin(dto.getTemperature().getMin());
      care.setTempMax(dto.getTemperature().getMax());
    }
    if (dto.getEnvHumidity() != null) {
      care.setEnvHumidityMin(dto.getEnvHumidity().getMin());
      care.setEnvHumidityMax(dto.getEnvHumidity().getMax());
    }
    if (dto.getSoilMoisture() != null) {
      care.setSoilMoistureMin(dto.getSoilMoisture().getMin());
      care.setSoilMoistureMax(dto.getSoilMoisture().getMax());
    }
  }

  private Double toDouble(Integer value) {
    return value != null ? value.doubleValue() : null;
  }

}

package com.smartgarden.integration.controller;

import com.smartgarden.integration.cache.PlantDeviceCache;
import com.smartgarden.integration.dto.device.IrrigationReadingDto;
import com.smartgarden.integration.dto.plant.PlantRequestDto;
import com.smartgarden.integration.dto.plant.PlantResponseDto;
import com.smartgarden.integration.service.PlantService;
import com.smartgarden.integration.service.ReadingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/plants")
@RequiredArgsConstructor
public class PlantController {

  private final PlantService plantService;
  private final ReadingService readingService;
  private final PlantDeviceCache plantDeviceCache;

  @GetMapping
  public ResponseEntity<List<PlantResponseDto>> listPlants() {
    return ResponseEntity.ok(plantService.listPlants());
  }

  @GetMapping("/{id}")
  public ResponseEntity<PlantResponseDto> getPlant(@PathVariable String id) {
    return ResponseEntity.ok(plantService.getPlant(id));
  }

  @PostMapping
  public ResponseEntity<PlantResponseDto> createPlant(@Valid @RequestBody PlantRequestDto dto) {
    return ResponseEntity.status(HttpStatus.CREATED).body(plantService.createPlant(dto));
  }

  @PutMapping("/{id}")
  public ResponseEntity<PlantResponseDto> updatePlant(
      @PathVariable String id,
      @Valid @RequestBody PlantRequestDto dto) {
    return ResponseEntity.ok(plantService.updatePlant(id, dto));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deletePlant(@PathVariable String id) {
    plantService.deletePlant(id);
    return ResponseEntity.noContent().build();
  }

  // Resolve plantId → deviceKey via cache populado pelas decisões de irrigação
  // @GetMapping("/{id}/readings")
  // public ResponseEntity<List<IrrigationReadingDto>>
  // getReadingsForPlant(@PathVariable String id) {
  // String deviceKey = plantDeviceCache.resolveDeviceKey(id)
  // .orElse(null);

  // // Dispositivo ainda não enviou leituras ou não há decisão de irrigação
  // // registrada.
  // if (deviceKey == null) {
  // log.debug("No deviceKey found for plantId={} — no readings available yet",
  // id);

  // // Retorna lista vazia — comportamento explícito, não silencioso.
  // return ResponseEntity.ok(List.of());
  // }

  // return ResponseEntity.ok(readingService.getReadingsForDevice(deviceKey));
  // }
  @GetMapping("/{id}/readings")
  public ResponseEntity<List<IrrigationReadingDto>> getReadingsForPlant(@PathVariable String id) {
    String deviceKey = plantDeviceCache.resolveDeviceKey(id).orElse(null);

    if (deviceKey == null) {
      log.debug("No deviceKey found for plantId={} — no readings available yet", id);
      return ResponseEntity.ok(List.of());
    }

    // Tenta o cache em memória (leituras recentes desde o último boot)
    List<IrrigationReadingDto> cached = readingService.getReadingsForDevice(deviceKey);
    if (!cached.isEmpty()) {
      return ResponseEntity.ok(cached);
    }

    // Cache vazio após restart → busca histórico no plant-management-service via
    // AMQP
    log.info("ReadingCache empty for deviceKey={} — fetching from domain via AMQP", deviceKey);
    try {
      List<IrrigationReadingDto> fromDomain = plantService.getReadingsByDeviceFromDomain(deviceKey);
      return ResponseEntity.ok(fromDomain);
    } catch (Exception e) {
      log.warn("AMQP fallback failed for deviceKey={}: {}", deviceKey, e.getMessage());
      return ResponseEntity.ok(List.of());
    }
  }
}

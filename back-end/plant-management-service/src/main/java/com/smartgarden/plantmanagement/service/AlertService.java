package com.smartgarden.plantmanagement.service;

import com.smartgarden.plantmanagement.dto.alert.AlertDto;
import com.smartgarden.plantmanagement.dto.irrigation.IrrigationDecisionDto;
import com.smartgarden.plantmanagement.model.AlertModel;
import com.smartgarden.plantmanagement.model.PlantModel;
import com.smartgarden.plantmanagement.model.ReadingModel;
import com.smartgarden.plantmanagement.repository.AlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {

  private final AlertRepository alertRepository;

  /**
   * Persiste um alerta de irrigação baseado na decisão do domínio.
   * Recebe {@link IrrigationDecisionDto} para ter acesso a todos os campos
   * relevantes (currentHumidity, targetHumidity, reason).
   */
  @Transactional
  public AlertModel createIrrigationAlert(ReadingModel reading,
      PlantModel plant,
      IrrigationDecisionDto decision) {
    String severity = Boolean.TRUE.equals(decision.getShouldIrrigate()) ? "CRITICAL" : "WARNING";
    String message = buildMessage(decision);

    AlertModel alert = AlertModel.builder()
        .plant(plant)
        .deviceKey(reading.getDeviceKey())
        .type(decision.getReason())
        .message(message)
        .severity(severity)
        .resolved(false)
        .build();

    AlertModel saved = alertRepository.save(alert);
    log.info("Alert created [type={}, device={}, severity={}]",
        decision.getReason(), reading.getDeviceKey(), severity);
    return saved;
  }

  @Transactional(readOnly = true)
  public List<AlertDto> getPendingAlerts() {
    return alertRepository.findByResolvedFalseOrderByCreatedAtDesc()
        .stream()
        .map(this::toDto)
        .toList();
  }

  @Transactional
  public void resolveAlert(String alertId) {
    alertRepository.findById(UUID.fromString(alertId))
        .ifPresent(a -> {
          a.setResolved(true);
          alertRepository.save(a);
        });
  }

  private AlertDto toDto(AlertModel m) {
    return AlertDto.builder()
        .id(m.getId().toString())
        .plantId(m.getPlant() != null ? m.getPlant().getId().toString() : null)
        .deviceKey(m.getDeviceKey())
        .type(m.getType())
        .message(m.getMessage())
        .severity(m.getSeverity())
        .resolved(m.getResolved())
        .createdAt(m.getCreatedAt())
        .build();
  }

  @Transactional
  public void deleteAlertsByPlantId(UUID plantId) {
    alertRepository.deleteByPlantId(plantId);
    log.info("Deleted alerts for plant id={}", plantId);
  }

  private String buildMessage(IrrigationDecisionDto decision) {
    return switch (decision.getReason()) {
      case "LOW_MOISTURE" -> "Soil moisture %.1f%% is below minimum %.1f%%. Target: %.1f%%."
          .formatted(
              decision.getCurrentHumidity(),
              decision.getMinHumidity(),
              decision.getTargetHumidity());
      case "HIGH_MOISTURE" -> "Soil moisture %.1f%% exceeds maximum %.1f%%. Check drainage."
          .formatted(decision.getCurrentHumidity(), decision.getMaxHumidity());
      case "NO_CARE_DATA" -> "No care data for plant. Using defaults. Humidity: %.1f%%."
          .formatted(decision.getCurrentHumidity());
      default -> "Moisture reading: %.1f%%."
          .formatted(decision.getCurrentHumidity() != null ? decision.getCurrentHumidity() : 0.0);
    };
  }
}

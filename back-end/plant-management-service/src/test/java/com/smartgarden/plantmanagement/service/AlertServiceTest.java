package com.smartgarden.plantmanagement.service;

import com.smartgarden.plantmanagement.dto.alert.AlertDto;
import com.smartgarden.plantmanagement.dto.irrigation.IrrigationDecisionDto;
import com.smartgarden.plantmanagement.model.AlertModel;
import com.smartgarden.plantmanagement.model.ReadingModel;
import com.smartgarden.plantmanagement.repository.AlertRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

  @Mock
  AlertRepository alertRepository;
  @InjectMocks
  AlertService alertService;

  @Test
  void shouldCreateCriticalAlertForLowMoisture() {
    ReadingModel reading = ReadingModel.builder()
        .deviceKey("esp-01").humidity(20.0).build();
    IrrigationDecisionDto decision = IrrigationDecisionDto.builder()
        .shouldIrrigate(true).reason("LOW_MOISTURE")
        .currentHumidity(20.0).minHumidity(30.0).targetHumidity(54.0).build();

    AlertModel saved = AlertModel.builder()
        .id(UUID.randomUUID()).type("LOW_MOISTURE").severity("CRITICAL").build();
    when(alertRepository.save(any())).thenReturn(saved);

    alertService.createIrrigationAlert(reading, null, decision);

    ArgumentCaptor<AlertModel> captor = ArgumentCaptor.forClass(AlertModel.class);
    verify(alertRepository).save(captor.capture());
    assertThat(captor.getValue().getType()).isEqualTo("LOW_MOISTURE");
    assertThat(captor.getValue().getSeverity()).isEqualTo("CRITICAL");
    assertThat(captor.getValue().getMessage()).contains("20.0").contains("30.0").contains("54.0");
  }

  @Test
  void shouldCreateWarningAlertForHighMoisture() {
    ReadingModel reading = ReadingModel.builder()
        .deviceKey("esp-01").humidity(90.0).build();
    IrrigationDecisionDto decision = IrrigationDecisionDto.builder()
        .shouldIrrigate(false).reason("HIGH_MOISTURE")
        .currentHumidity(90.0).maxHumidity(70.0).build();

    AlertModel saved = AlertModel.builder()
        .id(UUID.randomUUID()).type("HIGH_MOISTURE").severity("WARNING").build();
    when(alertRepository.save(any())).thenReturn(saved);

    alertService.createIrrigationAlert(reading, null, decision);

    ArgumentCaptor<AlertModel> captor = ArgumentCaptor.forClass(AlertModel.class);
    verify(alertRepository).save(captor.capture());
    assertThat(captor.getValue().getSeverity()).isEqualTo("WARNING");
  }

  @Test
  void getPendingAlerts_shouldReturnNonResolvedAlerts() {
    AlertModel alert = AlertModel.builder()
        .id(UUID.randomUUID()).type("LOW_MOISTURE")
        .message("Soil moisture 20% is below minimum 30%.").severity("CRITICAL")
        .resolved(false).build();

    when(alertRepository.findByResolvedFalseOrderByCreatedAtDesc())
        .thenReturn(List.of(alert));

    List<AlertDto> result = alertService.getPendingAlerts();
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getType()).isEqualTo("LOW_MOISTURE");
  }
}

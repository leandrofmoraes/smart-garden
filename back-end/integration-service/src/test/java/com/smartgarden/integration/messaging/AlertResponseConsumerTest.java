package com.smartgarden.integration.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.smartgarden.integration.cache.AlertCache;
import com.smartgarden.integration.dto.irrigation.IrrigationDecisionDto;
import com.smartgarden.integration.dto.messaging.AmqpPlantResponseDto;
import com.smartgarden.integration.messaging.consumer.AlertResponseConsumer;
import com.smartgarden.integration.service.IrrigationDecisionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertResponseConsumerTest {

  @Mock
  AlertCache alertCache;
  @Mock
  IrrigationDecisionService irrigationDecisionService;
  @Spy
  ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

  @InjectMocks
  AlertResponseConsumer consumer;

  @Test
  void shouldAlwaysAddToCacheAndRouteValidIrrigationDecision() {
    AmqpPlantResponseDto alert = alert(irrigationPayload("esp-01", true, 20.0, 54.0));

    consumer.consume(alert);

    verify(alertCache).addAlert(alert);

    ArgumentCaptor<IrrigationDecisionDto> captor = ArgumentCaptor.forClass(IrrigationDecisionDto.class);
    verify(irrigationDecisionService).process(captor.capture());

    IrrigationDecisionDto routed = captor.getValue();
    assertThat(routed.getDeviceId()).isEqualTo("esp-01");
    assertThat(routed.getShouldIrrigate()).isTrue();
    assertThat(routed.getTargetHumidity()).isEqualTo(54.0);
    assertThat(routed.getCurrentHumidity()).isEqualTo(20.0);
    assertThat(routed.getReason()).isEqualTo("LOW_MOISTURE");
  }

  @Test
  void shouldRouteEvenWhenShouldIrrigateIsFalse() {
    AmqpPlantResponseDto alert = alert(irrigationPayload("esp-01", false, 55.0, 54.0));

    consumer.consume(alert);

    verify(alertCache).addAlert(alert);
    verify(irrigationDecisionService).process(any(IrrigationDecisionDto.class));
  }

  @Test
  void shouldAddToCacheButNotRouteGenericAlertWithoutShouldIrrigate() {
    Map<String, Object> payload = Map.of(
        "message", "Planta precisa de atenção",
        "plantId", "plant-xyz");
    AmqpPlantResponseDto alert = alert(payload);

    consumer.consume(alert);

    verify(alertCache).addAlert(alert);
    verifyNoInteractions(irrigationDecisionService);
  }

  @Test
  void shouldNotRouteWhenDeviceIdIsMissing() {
    Map<String, Object> payload = Map.of(
        "shouldIrrigate", true,
        "targetHumidity", 54.0
    // deviceId ausente
    );
    AmqpPlantResponseDto alert = alert(payload);

    consumer.consume(alert);

    verify(alertCache).addAlert(alert);
    verifyNoInteractions(irrigationDecisionService);
  }

  @Test
  void shouldNotRouteWhenTargetHumidityIsMissing() {
    Map<String, Object> payload = Map.of(
        "shouldIrrigate", true,
        "deviceId", "esp-01");
    AmqpPlantResponseDto alert = alert(payload);

    consumer.consume(alert);

    verify(alertCache).addAlert(alert);
    verifyNoInteractions(irrigationDecisionService);
  }

  @Test
  void shouldAddToCacheWhenPayloadIsNull() {
    AmqpPlantResponseDto alert = AmqpPlantResponseDto.builder()
        .correlationId("corr-1")
        .success(false)
        .payload(null)
        .build();

    consumer.consume(alert);

    verify(alertCache).addAlert(alert);
    verifyNoInteractions(irrigationDecisionService);
  }

  @Test
  void cacheIsAlwaysFedEvenWhenServiceThrows() {
    doThrow(new RuntimeException("MQTT broker down"))
        .when(irrigationDecisionService).process(any());

    AmqpPlantResponseDto alert = alert(irrigationPayload("esp-01", true, 20.0, 54.0));

    consumer.consume(alert);

    verify(alertCache).addAlert(alert);
  }

  @Test
  void shouldAddToCacheEvenWhenDeserializationFails() {
    AmqpPlantResponseDto alert = AmqpPlantResponseDto.builder()
        .success(true)
        .payload("payload-que-nao-é-um-map")
        .build();

    consumer.consume(alert);

    verify(alertCache).addAlert(alert);
    verifyNoInteractions(irrigationDecisionService);
  }

  private AmqpPlantResponseDto alert(Object payload) {
    return AmqpPlantResponseDto.builder()
        .correlationId(null)
        .success(true)
        .payload(payload)
        .build();
  }

  private Map<String, Object> irrigationPayload(String deviceId, boolean shouldIrrigate,
      double current, double target) {
    return Map.of(
        "deviceId", deviceId,
        "shouldIrrigate", shouldIrrigate,
        "currentHumidity", current,
        "minHumidity", 30.0,
        "maxHumidity", 70.0,
        "targetHumidity", target,
        "reason", "LOW_MOISTURE");
  }
}
